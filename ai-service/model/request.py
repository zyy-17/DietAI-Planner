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


class TitleGenerateRequest(BaseModel):
    """让 LLM 为一次对话生成不超过 12 字的概括性标题。"""

    message: str
    response: str


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


class FoodLibraryItem(BaseModel):
    """食物库条目（由 Java 侧注入，约束 AI 只能从中选食物）。"""
    name: str
    category: Optional[str] = None
    calories: Optional[float] = None            # 每100g热量 kcal
    protein: Optional[float] = None            # 每100g蛋白质 g
    carbohydrate: Optional[float] = None       # 每100g碳水 g
    fat: Optional[float] = None                # 每100g脂肪 g


class MealPlanGenerateRequest(BaseModel):
    """AI 生成完整食谱请求。"""

    goal: str                                      # lose / gain / maintain
    days: int                                      # 3 或 7
    meals: List[str]                               # ["breakfast","lunch","dinner",...]
    idea: Optional[str] = None                     # 用户自由描述的想法/需求
    daily_calories: Optional[float] = None         # 每日目标热量（kcal），可空
    dislikes: List[str] = []                       # 忌口食物列表
    preferences: List[str] = []                   # 饮食偏好标签
    food_library: List[FoodLibraryItem] = []       # 食物库白名单（系统+自定义）
