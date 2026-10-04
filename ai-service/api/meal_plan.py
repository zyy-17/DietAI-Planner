from fastapi import APIRouter, HTTPException
import logging

from model.request import FoodReplaceRequest
from service.meal_plan_service import recommend_replacements

logger = logging.getLogger("dietai")
router = APIRouter()


@router.post("/api/meal-plan/replace")
async def meal_plan_replace_endpoint(request: FoodReplaceRequest):
    """单项替换：在候选食物里挑最合适的替代品（AI 为可选增强，失败时 Java 侧会退化排序）。"""
    try:
        return recommend_replacements(request)
    except Exception as e:
        logger.error(f"食物替换推荐失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))
