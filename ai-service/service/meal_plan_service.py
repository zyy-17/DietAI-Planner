"""单项食物替换（AI 辅助）。

食谱本身由用户在 App 里自己编写，「AI 生成整份食谱」那条路径已经撤掉，
这里只保留一个可选能力：用户在编辑食谱时点「AI 智能推荐」，
由模型在系统挑好的同类候选中排序并给出理由。

设计要点：
1. **模型只排序，不造数据**：候选食物由 Java 侧从食物库筛好（同类目 + 热量接近），
   模型不许自己发明食物；它给出的名字还要回 Java 侧按食物库再核一遍。
2. **只管名字与克数**：热量与三大营养素一律回 Java 侧按食物库换算。
3. **失败要说失败**：模型不可用或连续拿不到合法内容时抛错，由 Java 侧退化成
   「同类目 + 热量接近」的算法排序，并在响应里标明 aiUsed=false。
4. **预算要留够**：思维链模型的「思考过程」同样占用 max_tokens，
   用 .env 默认的 2048 容易思考占满预算、正式内容为空，所以这里单独放宽。
"""

import json
import logging
import re
from typing import List, Optional

from model.request import FoodReplaceRequest
from prompt.system_prompt import FOOD_REPLACE_PROMPT
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
