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


class MealPlanGenerateRequest(BaseModel):
    """多天膳食方案生成请求（Java 侧组装好用户档案与食物库候选后传入）。"""

    user_id: int
    goal: str = "maintain"                      # lose / gain / maintain
    days: int = 7                               # 3 / 7 / 14
    meals: List[str] = ["breakfast", "lunch", "dinner"]
    daily_calories: Optional[float] = None
    preferences: List[str] = []
    dislikes: List[str] = []
    extra_requirement: Optional[str] = None
    context: Optional[str] = None               # 用户档案文本快照
    # 候选食物字符串，格式「名称|每100g热量|蛋白/碳水/脂肪」
    candidate_foods: List[str] = []


class FoodReplaceRequest(BaseModel):
    """单项食物替换请求：让模型在候选里挑最合适的替代品。"""

    user_id: int
    food_name: str
    amount: float = 0
    reason: Optional[str] = None                # 用户不想吃的原因（可空）
    goal: Optional[str] = "maintain"
    meal_type: Optional[str] = None
    meal_desc: Optional[str] = None             # 这一餐已有哪些食物（帮模型选搭配）
    preferences: List[str] = []
    dislikes: List[str] = []
    candidates: List[str] = []
