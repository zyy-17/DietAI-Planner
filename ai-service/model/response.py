from pydantic import BaseModel
from typing import Optional, List, Any


class ChatResponse(BaseModel):
    response: str
    session_id: int
    fallback: Optional[bool] = None
    error: Optional[str] = None


class DietPlanResponse(BaseModel):
    target_calories: float
    meals: Optional[dict] = None
    ai_plan: Optional[str] = None
    summary: Optional[str] = None
    nutrition_analysis: Optional[dict] = None
    meal_plan: Optional[dict] = None
    suggestions: Optional[List[str]] = None


class HealthResponse(BaseModel):
    status: str
    service: str
    version: str
    model: str