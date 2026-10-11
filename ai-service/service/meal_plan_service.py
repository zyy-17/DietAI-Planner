"""食谱模块 AI 服务（替换 + 生成）。

- 替换：用户在编辑食谱时点「AI 智能推荐」，由模型在候选中排序并给出理由。
  模型只排序不造数据，候选由 Java 侧从食物库筛好。
- 生成：用户输入想法，AI 生成完整 N 天膳食计划（每天每餐的食物+克数）。
  食物只能从 Java 注入的白名单里选，营养值回 Java 按 food 表换算。

共同设计原则：
1. 模型只输出食物名与克数，热量/营养素一律回 Java 侧换算
2. 失败要说失败，由 Java 侧决定是否退化
3. 思维链模型的「思考过程」占用 max_tokens，需单独放宽预算
"""

import json
import logging
import re
from typing import List, Optional

from model.request import FoodReplaceRequest, MealPlanGenerateRequest
from prompt.system_prompt import FOOD_REPLACE_PROMPT, MEAL_PLAN_GENERATE_PROMPT
from service.llm_service import call_llm_text, get_model

logger = logging.getLogger("dietai")

GOAL_DESC = {"lose": "减脂", "gain": "增重", "maintain": "维持体重"}
MEAL_DESC = {"breakfast": "早餐", "lunch": "午餐", "dinner": "晚餐", "snack": "加餐"}

# 最多尝试几次：前两次强制 JSON 模式，最后一次放开限制。
# 不再叠加更深的重试——底层 call_llm_text 自己已经对网络错误做了重试，
# 两层都开满会让一次失败放大成十几次调用（云端 API 是按量计费的）。
REPLACE_ATTEMPTS = 3

# 单项替换的任务不复杂，但要给思维链模型留出思考余量。
# max_tokens 只是上限，模型没用到那么多不会多计费。
REPLACE_MAX_TOKENS = 4096


def _clean_json(raw: str) -> str:
    """剥掉 ``` 代码块围栏，并从夹杂解释文字的输出里抠出 JSON 主体。"""
    cleaned = (raw or "").strip()
    if cleaned.startswith("```"):
        lines = [l for l in cleaned.split("\n") if not l.strip().startswith("```")]
        cleaned = "\n".join(lines).strip()
    if not cleaned.startswith("{"):
        match = re.search(r"\{.*\}", cleaned, re.DOTALL)
        if match:
            cleaned = match.group()
    return cleaned


def _call_once(prompt: str, fmt: Optional[str], max_tokens: int) -> dict:
    """单次调用模型并把返回解析成 dict；任何问题都抛异常，由调用方决定是否重试。"""
    raw = call_llm_text(
        [{"role": "user", "content": prompt}], fmt=fmt, max_tokens=max_tokens
    )
    cleaned = _clean_json(raw)
    if not cleaned:
        raise ValueError("模型返回空内容")
    data = json.loads(cleaned)
    if not isinstance(data, dict):
        raise ValueError("模型返回的不是 JSON 对象")
    return data


def _fmt_float(value) -> Optional[float]:
    try:
        return round(float(value), 1)
    except (TypeError, ValueError):
        return None


def recommend_replacements(request: FoodReplaceRequest) -> dict:
    """在候选食物里挑替代项，返回 {recommendations:[{food, amount, reason}]}。"""
    if get_model() == "none":
        raise RuntimeError(
            "AI 模型不可用：请在 ai-service/.env 里配置 API_KEY，或确认本机 Ollama 已启动"
        )

    prompt = FOOD_REPLACE_PROMPT.format(
        food_name=request.food_name,
        amount=round(request.amount or 0),
        reason=request.reason or "单纯不想吃这个",
        goal_desc=GOAL_DESC.get(request.goal, "维持体重"),
        preferences="、".join(request.preferences) if request.preferences else "无特别偏好",
        dislikes="、".join(request.dislikes) if request.dislikes else "无",
        meal_desc=request.meal_desc or MEAL_DESC.get(request.meal_type, "这一餐"),
        candidate_section="\n".join(request.candidates) or "（没有候选食物）",
    )

    tag = f"替换「{request.food_name}」"
    last_error = None
    for attempt in range(REPLACE_ATTEMPTS):
        fmt = "json" if attempt < 2 else None
        try:
            data = _call_once(prompt, fmt, REPLACE_MAX_TOKENS)
            recommendations: List[dict] = []
            for item in (data.get("recommendations") or [])[:4]:
                if not isinstance(item, dict):
                    continue
                name = str(item.get("food") or "").strip()
                if not name:
                    continue
                recommendations.append({
                    "food": name,
                    "amount": _fmt_float(item.get("amount")),
                    "reason": str(item.get("reason") or "").strip(),
                })
            if not recommendations:
                raise ValueError("模型没有给出可用的替代食物")
            return {"recommendations": recommendations}
        except Exception as e:
            last_error = e
            logger.warning(f"{tag} 失败(尝试{attempt + 1}): {e}")
            # 预算被思维链占满属于「同一条件下必然重演」，再问几次只是白花钱
            if "被 max_tokens 截断" in str(e):
                raise RuntimeError(f"{tag}失败：{e}")

    raise RuntimeError(f"{tag}失败：{last_error}")


# ════════════════════════════════════════════════════════════════════════
#  AI 智能生成完整食谱
# ════════════════════════════════════════════════════════════════════════

# 生成任务的重试次数和 token 预算
GENERATE_ATTEMPTS = 3
GENERATE_MAX_TOKENS_3DAY = 4096     # 3天计划足够
GENERATE_MAX_TOKENS_7DAY = 8192     # 7天计划需要更多输出空间


def _build_food_library_section(food_library: list) -> str:
    """把食物库列表格式化为 Prompt 文本段。"""
    if not food_library:
        return "（未提供食物库）"
    lines = []
    for item in food_library:
        name = getattr(item, "name", item.get("name", "??"))
        cal = getattr(item, "calories", item.get("calories")) or "-"
        pro = getattr(item, "protein", item.get("protein")) or "-"
        carb = getattr(item, "carbohydrate", item.get("carbohydrate")) or "-"
        fat = getattr(item, "fat", item.get("fat")) or "-"
        cat = getattr(item, "category", item.get("category"))
        cat_str = f" [{cat}]" if cat else ""
        lines.append(f"- {name}{cat_str} | {cal} kcal | 蛋白质{pro}g / 碳水{carb}g / 脂肪{fat}g (每100g)")
    return "\n".join(lines)


def generate_meal_plan(request: MealPlanGenerateRequest) -> dict:
    """AI 生成完整的 N 天膳食计划，返回 { days: [{ day_index, meals: { ... } }] }。"""
    if get_model() == "none":
        raise RuntimeError(
            "AI 模型不可用：请在 ai-service/.env 里配置 API_KEY，或确认本机 Ollama 已启动"
        )

    goal_desc = GOAL_DESC.get(request.goal, "维持体重")
    meal_labels = [MEAL_DESC.get(m, m) for m in request.meals]

    # 组装 Prompt 各段落
    daily_calories_section = (
        f"【每日目标热量】{int(request.daily_calories)} kcal"
        if request.daily_calories
        else ""
    )
    idea_section = (
        f"【用户想法/需求】{request.idea}"
        if request.idea and request.idea.strip()
        else ""
    )

    prompt = MEAL_PLAN_GENERATE_PROMPT.format(
        days=request.days,
        goal_desc=goal_desc,
        meals_desc="、".join(meal_labels),
        daily_calories_section=daily_calories_section,
        idea_section=idea_section,
        preferences="、".join(request.preferences) if request.preferences else "无特别偏好",
        dislikes="、".join(request.dislikes) if request.dislikes else "无",
        food_library_section=_build_food_library_section(request.food_library),
        meals_keys=", ".join(request.meals),
    )

    # 根据周期选择 token 预算
    max_tokens = GENERATE_MAX_TOKENS_7DAY if request.days >= 7 else GENERATE_MAX_TOKENS_3DAY

    tag = f"生成{request.days}天{goal_desc}食谱"
    last_error = None
    for attempt in range(GENERATE_ATTEMPTS):
        fmt = "json" if attempt < 2 else None
        try:
            data = _call_once(prompt, fmt, max_tokens)

            # 校验并清洗返回结构
            raw_days = data.get("days") or []
            if not isinstance(raw_days, list) or len(raw_days) != request.days:
                raise ValueError(f"天数不匹配：期望 {request.days} 天，得到 {len(raw_days)} 天")

            result_days = []
            for day_data in raw_days:
                if not isinstance(day_data, dict):
                    continue
                day_index = day_data.get("day_index", len(result_days) + 1)
                meals_raw = day_data.get("meals") or {}
                cleaned_meals = {}
                for meal_type in request.meals:
                    items = meals_raw.get(meal_type) or []
                    cleaned_items = []
                    for it in items:
                        if not isinstance(it, dict):
                            continue
                        fname = str(it.get("food") or "").strip()
                        if not fname:
                            continue
                        amt = _fmt_float(it.get("amount"))
                        if amt is None or amt <= 0:
                            continue
                        cleaned_items.append({"food": fname, "amount": amt})
                    if cleaned_items:
                        cleaned_meals[meal_type] = cleaned_items
                if cleaned_meals:
                    result_days.append({
                        "day_index": day_index,
                        "meals": cleaned_meals,
                    })

            if not result_days:
                raise ValueError("模型没有生成任何有效的餐食数据")

            return {"days": result_days}

        except Exception as e:
            last_error = e
            logger.warning(f"{tag} 失败(尝试{attempt + 1}): {e}")
            if "被 max_tokens 截断" in str(e):
                raise RuntimeError(f"{tag}失败：{e}")

    raise RuntimeError(f"{tag}失败：{last_error}")
