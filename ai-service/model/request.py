from pydantic import BaseModel
from typing import Optional, List


class ChatMessage(BaseModel):
    message: str
    context: Optional[str] = None
    session_id: Optional[int] = None
    user_id: Optional[int] = None


class ChatHistoryItem(BaseModel):
    role: str
    content: str


class ChatWithHistoryRequest(BaseModel):
    message: str
    context: Optional[str] = None
    session_id: Optional[int] = None
    user_id: Optional[int] = None
    history: Optional[List[ChatHistoryItem]] = None


class DietPlanRequest(BaseModel):
    user_id: int
    target_calories: Optional[float] = None
    diet_goal: Optional[str] = None
    remaining_calories: Optional[float] = None
    context: Optional[str] = None
    candidate_foods: Optional[List[str]] = None


class MealItem(BaseModel):
    food: str
    amount: float
    calories: float


class MealPlan(BaseModel):
    breakfast: Optional[List[MealItem]] = []
    lunch: Optional[List[MealItem]] = []
    dinner: Optional[List[MealItem]] = []
    snack: Optional[List[MealItem]] = []


class NutritionAnalysis(BaseModel):
    calories_remaining: Optional[float] = 0
    protein_remaining: Optional[float] = 0
    fat_remaining: Optional[float] = 0


class StructuredDietPlan(BaseModel):
    summary: str = ""
    nutrition_analysis: Optional[NutritionAnalysis] = None
    meal_plan: Optional[MealPlan] = None
    suggestions: Optional[List[str]] = []
    ai_plan: Optional[str] = None