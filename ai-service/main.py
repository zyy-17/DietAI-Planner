from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional
import uvicorn

app = FastAPI(title="DietAI AI Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class ChatMessage(BaseModel):
    message: str
    context: Optional[str] = None
    session_id: Optional[int] = None
    user_id: Optional[int] = None


class DietPlanRequest(BaseModel):
    user_id: int
    target_calories: Optional[float] = None
    diet_goal: Optional[str] = None
    remaining_calories: Optional[float] = None


SYSTEM_PROMPT = """你是一个专业的AI膳食营养助手，名叫"智慧膳食"。你的职责是：
1. 根据用户的健康数据（身高、体重、年龄、活动水平、饮食目标）提供个性化膳食建议
2. 帮助用户分析每日营养摄入是否合理
3. 推荐适合用户目标（减脂/维持/增肌）的食物和食谱
4. 回答用户关于食物营养、热量、健康饮食的问题

注意事项：
- 所有建议应基于科学营养学知识
- 推荐食物时考虑营养均衡（碳水50%、蛋白质20%、脂肪30%）
- 如果用户有特殊健康状况，建议咨询专业医生或营养师
- 回答要简洁实用，给出具体可操作的建议
"""


def build_prompt(message: str, context: Optional[str] = None) -> str:
    prompt = SYSTEM_PROMPT
    if context:
        prompt += f"\n\n当前用户健康数据：\n{context}\n"
    prompt += f"\n用户提问：{message}\n\n请给出专业、实用的回答："
    return prompt


def generate_diet_response(message: str, context: Optional[str] = None) -> str:
    prompt = build_prompt(message, context)

    if "吃什么" in message or "推荐" in message or "食谱" in message:
        if context and "剩余" in context:
            return generate_meal_recommendation(context)
        return """根据营养均衡原则，为您推荐以下膳食搭配：

🍽️ 推荐餐食方案：
• 主食：糙米饭/全麦面包（控制份量约100-150g）
• 蛋白质：鸡胸肉150g / 清蒸鱼200g / 豆腐200g
• 蔬菜：西兰花、菠菜等深色蔬菜200-300g
• 优质脂肪：少量坚果或橄榄油

📊 营养配比参考：
- 碳水化合物：50%（约250-325g）
- 蛋白质：20%（约65-80g）
- 脂肪：30%（约47-72g）

💡 小贴士：每餐先吃蔬菜，再吃蛋白质，最后吃主食，有助于控制血糖和饱腹感。"""

    elif "热量" in message or "卡路里" in message or "多少" in message:
        return """关于热量摄入，以下是一些参考建议：

📊 每日热量需求计算：
• 基础代谢率(BMR)：根据Mifflin-St Jeor公式计算
• 每日总消耗(TDEE) = BMR × 活动系数
• 减脂目标：TDEE × 0.8（热量缺口约20%）
• 维持体重：TDEE × 1.0
• 增肌目标：TDEE × 1.15（热量盈余约15%）

🍽️ 常见食物热量参考（每100g）：
• 白米饭：116 kcal
• 鸡胸肉：133 kcal
• 西兰花：36 kcal
• 苹果：53 kcal
• 鸡蛋：144 kcal

建议您在"今日饮食"页面记录每日摄入，系统会自动计算并提醒您剩余可摄入热量。"""

    elif "减脂" in message or "减肥" in message or "瘦" in message:
        return """科学减脂饮食建议：

🎯 减脂核心原则：
• 热量缺口：每日摄入比消耗少300-500千卡
• 高蛋白：保护肌肉，提高饱腹感（1.6-2.2g/kg体重）
• 适量碳水：优先选择低GI食物
• 健康脂肪：不刻意追求极低脂肪

📋 减脂一日食谱参考：
早餐：燕麦片50g + 鸡蛋1个 + 牛奶250ml（约350kcal）
午餐：糙米饭100g + 鸡胸肉150g + 西兰花200g（约450kcal）
晚餐：清蒸鱼150g + 菠菜200g + 少量红薯（约350kcal）
加餐：苹果1个 或 坚果一小把（约100kcal）

⚠️ 注意：减脂不等于节食，保证营养均衡才是可持续的健康方式！"""

    elif "增肌" in message or "肌肉" in message:
        return """科学增肌饮食建议：

💪 增肌核心原则：
• 热量盈余：每日摄入比消耗多200-400千卡
• 高蛋白：每kg体重1.6-2.2g蛋白质
• 充足碳水：为训练提供能量（4-6g/kg体重）
• 训练前后营养补充

📋 增肌一日食谱参考：
早餐：全麦面包2片 + 鸡蛋3个 + 牛奶300ml（约550kcal）
午餐：米饭200g + 牛肉200g + 西兰花200g（约650kcal）
加餐：蛋白粉1勺 + 香蕉1根（约250kcal）
晚餐：糙米饭150g + 鸡胸肉200g + 番茄200g（约550kcal）
睡前：酪蛋白/牛奶200ml（约150kcal）

💡 建议：训练后30分钟内补充蛋白质+碳水，促进肌肉恢复和生长。"""

    else:
        return f"""您好！我是您的智能膳食助手，很高兴为您服务 🍎

我可以帮您：
• 🍽️ 制定个性化膳食计划（减脂/维持/增肌）
• 📊 分析每日营养摄入是否合理
• 🔍 查询食物热量和营养成分
• 💡 提供健康饮食建议

请告诉我您想了解什么？例如：
- "今天晚餐吃什么好？"
- "帮我制定减脂食谱"
- "鸡胸肉的热量是多少？"
- "我还能吃多少热量？"
"""


def generate_meal_recommendation(context: str) -> str:
    return f"""根据您的健康数据，为您智能推荐：

{context}

🍽️ 个性化推荐方案：
• 建议选择高蛋白、低脂肪的食物组合
• 主食优先选择全谷物，控制份量
• 每餐蔬菜不少于200g
• 避免高糖饮料和加工食品

如需更详细的膳食计划，请告诉我您的具体需求！"""


@app.post("/api/chat")
async def chat(chat_message: ChatMessage):
    try:
        response = generate_diet_response(chat_message.message, chat_message.context)
        return {"response": response, "session_id": chat_message.session_id}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/diet-plan")
async def generate_diet_plan(request: DietPlanRequest):
    try:
        target = request.target_calories or 2000
        goal = request.diet_goal or "maintain"

        if goal == "lose":
            target = target * 0.8
        elif goal == "gain":
            target = target * 1.15

        breakfast_cal = target * 0.30
        lunch_cal = target * 0.40
        dinner_cal = target * 0.25
        snack_cal = target * 0.05

        plan = {
            "target_calories": round(target, 0),
            "meals": {
                "breakfast": {"calories": round(breakfast_cal, 0), "suggestion": "燕麦+鸡蛋+牛奶"},
                "lunch": {"calories": round(lunch_cal, 0), "suggestion": "糙米饭+鸡胸肉+蔬菜"},
                "dinner": {"calories": round(dinner_cal, 0), "suggestion": "清蒸鱼+蔬菜+少量主食"},
                "snack": {"calories": round(snack_cal, 0), "suggestion": "水果或坚果"},
            }
        }
        return plan
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/health")
async def health_check():
    return {"status": "ok", "service": "DietAI AI Service"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)