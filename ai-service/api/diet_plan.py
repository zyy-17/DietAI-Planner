from fastapi import APIRouter, HTTPException
import logging
from model.request import DietPlanRequest
from service.diet_plan_service import generate_diet_plan, generate_structured_suggestion

logger = logging.getLogger("dietai")
router = APIRouter()


@router.post("/api/diet-plan")
async def diet_plan_endpoint(request: DietPlanRequest):
    try:
        return generate_diet_plan(request)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/api/diet-plan/structured")
async def structured_diet_plan_endpoint(request: DietPlanRequest):
    try:
        remaining = request.remaining_calories or 0
        target = request.target_calories or 2000
        goal = request.diet_goal or "maintain"

        if goal == "lose":
            target *= 0.8
        elif goal == "gain":
            target *= 1.15

        actual = target - remaining
        protein_gap = (target * 0.20 / 4) - (actual * 0.20 / 4)

        candidates = request.candidate_foods or []

        return generate_structured_suggestion(
            remaining_calories=remaining,
            protein_gap=protein_gap,
            diet_goal=goal,
            candidate_foods=candidates,
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))