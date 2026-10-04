from fastapi import APIRouter, HTTPException
import logging

from model.request import FoodReplaceRequest, MealPlanGenerateRequest
from service.meal_plan_service import generate_meal_plan, recommend_replacements

logger = logging.getLogger("dietai")
router = APIRouter()


@router.post("/api/meal-plan/generate")
async def meal_plan_generate_endpoint(request: MealPlanGenerateRequest):
    """按用户档案 + 目标 + 偏好忌口 + 食物库候选，生成多天膳食方案。

    返回的每条食物只有 food(名称) 与 amount(克数)，营养值由 Java 侧按食物库换算。
    """
    try:
        return generate_meal_plan(request)
    except Exception as e:
        logger.error(f"膳食方案生成失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/api/meal-plan/replace")
async def meal_plan_replace_endpoint(request: FoodReplaceRequest):
    """单项替换：在候选食物里挑最合适的替代品。"""
    try:
        return recommend_replacements(request)
    except Exception as e:
        logger.error(f"食物替换推荐失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))
