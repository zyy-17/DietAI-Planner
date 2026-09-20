import json
import re
import logging
from typing import Optional, List
from service.llm_service import call_ollama_text, get_model
from prompt.system_prompt import DIET_PLAN_PROMPT_TEMPLATE, STRUCTURED_DIET_PLAN_PROMPT
from model.request import DietPlanRequest, StructuredDietPlan
from config.settings import MAX_RETRIES

logger = logging.getLogger("dietai")

FALLBACK_PLAN = "AI 服务暂不可用，请稍后重试。"


def _clean_json_response(raw: str) -> str:
    cleaned = raw.strip()
    if cleaned.startswith("```"):
        lines = cleaned.split("\n")
        lines = [l for l in lines if not l.strip().startswith("```")]
        cleaned = "\n".join(lines).strip()
    if not cleaned.startswith("{"):
        match = re.search(r"\{.*\}", cleaned, re.DOTALL)
        if match:
            cleaned = match.group()
    return cleaned


def generate_diet_plan(request: DietPlanRequest) -> dict:
    try:
        target = request.target_calories or 2000
        goal = request.diet_goal or "maintain"
        goal_desc = {"lose": "减脂", "maintain": "维持体重", "gain": "增肌"}.get(goal, "维持体重")

        if goal == "lose":
            target *= 0.8
        elif goal == "gain":
            target *= 1.15

        user_context = request.context or "无额外用户信息"

        candidate_section = ""
        candidate_instruction = "优先从候选食物中选择"
        if request.candidate_foods:
            candidate_section = f"【候选食物(算法推荐)】{', '.join(request.candidate_foods)}"
            candidate_instruction = f"必须优先从以下候选食物中选择：{', '.join(request.candidate_foods)}"

        prompt = DIET_PLAN_PROMPT_TEMPLATE.format(
            target_calories=round(target),
            diet_goal=goal_desc,
            candidate_section=candidate_section,
            user_context=user_context,
            candidate_instruction=candidate_instruction,
        )

        plan = {
            "target_calories": round(target, 0),
            "meals": {
                "breakfast": {"calories": round(target * 0.30, 0), "suggestion": ""},
                "lunch": {"calories": round(target * 0.40, 0), "suggestion": ""},
                "dinner": {"calories": round(target * 0.25, 0), "suggestion": ""},
                "snack": {"calories": round(target * 0.05, 0), "suggestion": ""},
            }
        }

        if get_model() != "none":
            try:
                ai_plan = call_ollama_text([{"role": "user", "content": prompt}])
                plan["ai_plan"] = ai_plan
            except Exception as e:
                logger.error(f"膳食规划AI生成失败: {e}")
                plan["ai_plan"] = FALLBACK_PLAN

        return plan
    except Exception as e:
        logger.error(f"生成膳食计划失败: {e}")
        raise


def generate_structured_suggestion(remaining_calories: float, protein_gap: float,
                                   diet_goal: str, candidate_foods: List[str]) -> dict:
    result = {
        "summary": "",
        "nutrition_analysis": {
            "calories_remaining": remaining_calories,
            "protein_remaining": protein_gap,
        },
        "suggestions": [],
    }

    if get_model() == "none":
        result["summary"] = "AI服务不可用"
        result["suggestions"] = ["请检查Ollama服务是否正常运行"]
        return result

    prompt = STRUCTURED_DIET_PLAN_PROMPT.format(
        remaining_calories=round(remaining_calories),
        protein_gap=round(protein_gap, 1),
        diet_goal=diet_goal,
        candidate_foods=", ".join(candidate_foods) if candidate_foods else "无",
    )

    use_json_mode = True
    for attempt in range(MAX_RETRIES + 1):
        try:
            if attempt < 3 and use_json_mode:
                raw = call_ollama_text([{"role": "user", "content": prompt}], fmt="json")
            else:
                raw = call_ollama_text([{"role": "user", "content": prompt}])

            if not raw or not raw.strip():
                logger.warning(f"AI返回空响应(尝试{attempt + 1})")
                continue

            cleaned = _clean_json_response(raw)
            if not cleaned:
                logger.warning(f"JSON清理后为空(尝试{attempt + 1})")
                continue

            parsed = json.loads(cleaned)
            validated = StructuredDietPlan(**parsed)
            result["summary"] = validated.summary
            result["suggestions"] = validated.suggestions or []
            logger.info(f"结构化输出解析成功(尝试{attempt + 1})")
            return result
        except json.JSONDecodeError as e:
            logger.warning(f"JSON解析失败(尝试{attempt + 1}): {e}")
            if attempt == 2:
                use_json_mode = False
                logger.info("JSON模式3次失败，切换为普通模式+正则提取")
        except Exception as e:
            logger.warning(f"结构化输出解析失败(尝试{attempt + 1}): {e}")

    result["summary"] = "营养分析完成（AI输出格式异常，已降级）"
    result["suggestions"] = [
        f"剩余热量{round(remaining_calories)}kcal，蛋白质缺口{round(protein_gap, 1)}g",
        "建议增加优质蛋白质摄入",
        "控制高脂食物摄入",
    ]
    return result