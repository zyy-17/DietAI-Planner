"""多天膳食方案生成 & 单项食物替换。

设计要点：
1. **分批编排**：一次要 14 天会让模型输出超出 max_tokens 而被截断，
   所以按 BATCH_DAYS 天一批调用，每批都是完整的小 JSON，再拼起来。
2. **只管食物名与克数**：热量与三大营养素由 Java 侧按食物库精确换算，
   模型只负责「搭配」，不负责算数——避免模型编造热量污染数据。
3. **失败要说失败**：模型不可用或多次重试仍拿不到合法内容时直接抛错，
   由上层返回明确提示，不返回看起来像正常方案的假数据。
4. **「重试」覆盖到校验之后**：模型少排了一天、或某天食物全是空项，也算这次生成失败，
   会连同调用一起重试，而不是把残缺方案返回给用户。
"""

import json
import logging
import re
from typing import List, Optional, Tuple

from model.request import FoodReplaceRequest, MealPlanGenerateRequest
from prompt.system_prompt import FOOD_REPLACE_PROMPT, MEAL_PLAN_PROMPT
from service.llm_service import call_llm_text, get_model

logger = logging.getLogger("dietai")

GOAL_DESC = {"lose": "减脂", "gain": "增重", "maintain": "维持体重"}
MEAL_DESC = {"breakfast": "早餐", "lunch": "午餐", "dinner": "晚餐", "snack": "加餐"}

# 单次让模型编排的天数
BATCH_DAYS = 3

# 每个食物名在「避免重复」提示里最多列出这么多，控制上下文长度
MAX_USED_HINT = 40

# 一批最多尝试几次：前两次强制 JSON 模式，最后一次放开限制。
# 不再叠加更深的重试——底层 call_llm_text 自己已经对网络错误做了重试，
# 两层都开满会让一次失败放大成十几次调用（云端 API 是按量计费的）。
BATCH_ATTEMPTS = 3


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


def _call_once(prompt: str, fmt: Optional[str]) -> dict:
    """单次调用模型并把返回解析成 dict；任何问题都抛异常，由调用方决定是否重试。"""
    raw = call_llm_text([{"role": "user", "content": prompt}], fmt=fmt)
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


def _normalize_meals(raw_meals: dict, meal_keys: List[str]) -> dict:
    """只保留要求的那几餐，丢弃空项与非正数克数。"""
    meals_out = {}
    for key in meal_keys:
        items = (raw_meals or {}).get(key) or []
        cleaned = []
        for item in items:
            if not isinstance(item, dict):
                continue
            name = str(item.get("food") or "").strip()
            amount = _fmt_float(item.get("amount"))
            if not name or amount is None or amount <= 0:
                continue
            cleaned.append({"food": name, "amount": amount})
        if cleaned:
            meals_out[key] = cleaned
    return meals_out


def _parse_batch(data: dict, start_day: int, end_day: int,
                 meal_keys: List[str]) -> Tuple[List[dict], str]:
    """把模型输出规整成 [{day, meals}]；缺任何一天都算这批失败。"""
    by_day = {}
    for raw_day in (data.get("days") or []):
        if not isinstance(raw_day, dict):
            continue
        day_no = _fmt_float(raw_day.get("day"))
        if day_no is None:
            continue
        by_day[int(day_no)] = _normalize_meals(raw_day.get("meals") or {}, meal_keys)

    missing = [d for d in range(start_day, end_day + 1) if not by_day.get(d)]
    if missing:
        raise ValueError("模型漏排了第 %s 天" % "、".join(str(d) for d in missing))

    days = [{"day": d, "meals": by_day[d]} for d in range(start_day, end_day + 1)]
    return days, str(data.get("summary") or "").strip()


def _generate_batch(request: MealPlanGenerateRequest, start_day: int, end_day: int,
                    used_foods: List[str]) -> Tuple[List[dict], str]:
    """编排第 start_day ~ end_day 天。调用失败或内容不合规都会重试整批。"""
    seen: List[str] = []
    for name in used_foods:
        if name not in seen:
            seen.append(name)

    history_hint = ""
    if seen:
        history_hint = ("\n【已经安排过的食物（这几天不要重复出现）】"
                        + "、".join(seen[:MAX_USED_HINT]) + "\n")

    prompt = MEAL_PLAN_PROMPT.format(
        start_day=start_day,
        end_day=end_day,
        user_context=request.context or "无额外用户信息",
        goal_desc=GOAL_DESC.get(request.goal, "维持体重"),
        meal_desc="、".join(MEAL_DESC.get(m, m) for m in request.meals) or "早餐、午餐、晚餐",
        daily_calories=round(request.daily_calories or 2000),
        preferences="、".join(request.preferences) if request.preferences else "无特别偏好",
        dislikes="、".join(request.dislikes) if request.dislikes else "无",
        extra=request.extra_requirement or "无",
        history_hint=history_hint,
        candidate_section="\n".join(request.candidate_foods) or "（食物库为空）",
    )

    tag = f"膳食方案第{start_day}-{end_day}天"
    last_error = None
    for attempt in range(BATCH_ATTEMPTS):
        # 前两轮强制 JSON 模式；最后一次放开限制，靠正则从自由文本里抠 JSON
        fmt = "json" if attempt < 2 else None
        try:
            data = _call_once(prompt, fmt)
            days, summary = _parse_batch(data, start_day, end_day, request.meals)
            logger.info(f"{tag} 解析成功(尝试{attempt + 1})")
            return days, summary
        except json.JSONDecodeError as e:
            last_error = f"JSON解析失败: {e}"
            logger.warning(f"{tag} {last_error}(尝试{attempt + 1}/{BATCH_ATTEMPTS})")
        except Exception as e:
            last_error = e
            logger.warning(f"{tag} 失败(尝试{attempt + 1}/{BATCH_ATTEMPTS}): {e}")

    raise RuntimeError(f"{tag}失败：{last_error}")


def generate_meal_plan(request: MealPlanGenerateRequest) -> dict:
    """按天分批生成完整方案，返回 {summary, days:[{day, meals:{...}}]}。"""
    if get_model() == "none":
        raise RuntimeError(
            "AI 模型不可用：请在 ai-service/.env 里配置 API_KEY，或确认本机 Ollama 已启动"
        )

    total_days = max(1, int(request.days or 7))
    days: List[dict] = []
    used_foods: List[str] = []
    summary = ""
    start = 1
    while start <= total_days:
        end = min(start + BATCH_DAYS - 1, total_days)
        batch, batch_summary = _generate_batch(request, start, end, used_foods)
        if not summary and batch_summary:
            summary = batch_summary
        for day in batch:
            days.append(day)
            for items in day["meals"].values():
                used_foods.extend(i["food"] for i in items)
        start = end + 1

    item_count = sum(len(v) for d in days for v in d["meals"].values())
    logger.info(f"膳食方案生成完成：{total_days} 天，共 {item_count} 条食物")
    return {"summary": summary, "days": days}


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
    for attempt in range(BATCH_ATTEMPTS):
        fmt = "json" if attempt < 2 else None
        try:
            data = _call_once(prompt, fmt)
            recommendations = []
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

    raise RuntimeError(f"{tag}失败：{last_error}")
