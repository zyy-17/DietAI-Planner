from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List
from ollama import chat as ollama_chat
from ollama import ChatResponse
import uvicorn

app = FastAPI(title="DietAI AI Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

OLLAMA_MODEL = "qwen2.5-coder:7b"


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


SYSTEM_PROMPT = """你是一个专业的AI膳食营养助手，名叫"智慧膳食"。你的职责是：
1. 根据用户的健康数据（身高、体重、年龄、活动水平、饮食目标、饮食偏好）提供个性化膳食建议
2. 帮助用户分析每日营养摄入是否合理，指出营养缺口和改进方向
3. 推荐适合用户目标（减脂/维持/增肌）的食物和食谱，优先考虑用户的饮食偏好
4. 回答用户关于食物营养、热量、健康饮食的问题
5. 如果用户提供了今日摄入数据，结合剩余热量和营养素缺口给出精准建议

个性化规则：
- 仔细阅读用户画像数据，根据性别、年龄、体重、身高给出针对性建议
- 根据饮食偏好调整推荐（如偏好清淡则少油少盐，偏好中式则推荐中式食谱）
- 根据活动水平判断热量需求，久坐人群适当降低碳水比例
- 如果用户有饮食偏好（如素食、低糖、无辣等），严格遵守
- 根据剩余热量和营养素缺口推荐具体食物和份量

注意事项：
- 所有建议应基于科学营养学知识
- 推荐食物时考虑营养均衡（碳水50%、蛋白质20%、脂肪30%）
- 如果用户有特殊健康状况，建议咨询专业医生或营养师
- 回答要简洁实用，给出具体可操作的建议
- 合适的时候可以用🍎🥗💪🍽️📊这类emoji让回复更生动
- 用亲切自然的语气，像朋友聊天一样
- 称呼用户时如果知道姓名就用姓名
"""


def build_system_prompt(context: Optional[str] = None) -> str:
    prompt = SYSTEM_PROMPT
    if context:
        prompt += f"\n\n当前用户健康数据：\n{context}\n"
    return prompt


def call_ollama(message: str, context: Optional[str] = None,
                history: Optional[List[ChatHistoryItem]] = None) -> str:
    system_content = build_system_prompt(context)

    messages = [{'role': 'system', 'content': system_content}]

    if history:
        for item in history:
            messages.append({'role': item.role, 'content': item.content})
        last_role = history[-1].role if history else None
        if last_role != 'user':
            messages.append({'role': 'user', 'content': message})
    else:
        messages.append({'role': 'user', 'content': message})

    response: ChatResponse = ollama_chat(
        model=OLLAMA_MODEL,
        messages=messages,
        stream=False
    )

    return response.message.content


def call_ollama_stream(message: str, context: Optional[str] = None,
                       history: Optional[List[ChatHistoryItem]] = None):
    system_content = build_system_prompt(context)

    messages = [{'role': 'system', 'content': system_content}]

    if history:
        for item in history:
            messages.append({'role': item.role, 'content': item.content})
        last_role = history[-1].role if history else None
        if last_role != 'user':
            messages.append({'role': 'user', 'content': message})
    else:
        messages.append({'role': 'user', 'content': message})

    response: ChatResponse = ollama_chat(
        model=OLLAMA_MODEL,
        messages=messages,
        stream=True
    )

    full_response = ""
    for chunk in response:
        if chunk.message.content is not None:
            full_response += chunk.message.content

    return full_response


def generate_fallback_response(message: str, context: Optional[str] = None) -> str:
    if "吃什么" in message or "推荐" in message or "食谱" in message:
        return """根据营养均衡原则，为您推荐以下膳食搭配：

🍽️ 推荐餐食方案：
• 主食：糙米饭/全麦面包（控制份量约100-150g）
• 蛋白质：鸡胸肉150g / 清蒸鱼200g / 豆腐200g
• 蔬菜：西兰花、菠菜等深色蔬菜200-300g
• 优质脂肪：少量坚果或橄榄油

📊 营养配比参考：碳水50%、蛋白质20%、脂肪30%

💡 小贴士：每餐先吃蔬菜，再吃蛋白质，最后吃主食，有助于控制血糖和饱腹感。"""

    elif "热量" in message or "卡路里" in message:
        return """📊 每日热量需求 = BMR × 活动系数
• 减脂：TDEE × 0.8
• 维持：TDEE × 1.0
• 增肌：TDEE × 1.15

建议在"今日饮食"页面记录每日摄入，系统会自动计算剩余可摄入热量。"""

    elif "减脂" in message or "减肥" in message or "瘦" in message:
        return """🎯 减脂核心：热量缺口300-500kcal/天，高蛋白保护肌肉，优先低GI碳水。
⚠️ 减脂≠节食，营养均衡才是可持续的健康方式！"""

    elif "增肌" in message or "肌肉" in message:
        return """💪 增肌核心：热量盈余200-400kcal/天，蛋白质1.6-2.2g/kg体重，训练后30分钟内补充蛋白+碳水。"""

    else:
        return """您好！我是您的智能膳食助手 🍎\n我可以帮您制定膳食计划、分析营养摄入、查询食物热量。请告诉我您想了解什么？"""


@app.post("/api/chat")
async def chat(chat_message: ChatMessage):
    try:
        response = call_ollama(
            message=chat_message.message,
            context=chat_message.context
        )
        return {"response": response, "session_id": chat_message.session_id}
    except Exception as e:
        fallback = generate_fallback_response(chat_message.message, chat_message.context)
        return {"response": fallback, "session_id": chat_message.session_id, "fallback": True, "error": str(e)}


@app.post("/api/chat/history")
async def chat_with_history(request: ChatWithHistoryRequest):
    try:
        response = call_ollama(
            message=request.message,
            context=request.context,
            history=request.history
        )
        return {"response": response, "session_id": request.session_id}
    except Exception as e:
        fallback = generate_fallback_response(request.message, request.context)
        return {"response": fallback, "session_id": request.session_id, "fallback": True, "error": str(e)}


@app.post("/api/diet-plan")
async def generate_diet_plan(request: DietPlanRequest):
    try:
        target = request.target_calories or 2000
        goal = request.diet_goal or "maintain"

        goal_desc = {"lose": "减脂", "maintain": "维持体重", "gain": "增肌"}.get(goal, "维持体重")

        if goal == "lose":
            target = target * 0.8
        elif goal == "gain":
            target = target * 1.15

        prompt = f"""请为以下用户生成一日膳食计划：
- 每日目标热量：{round(target)}kcal
- 饮食目标：{goal_desc}
- 要求：给出早中晚三餐+加餐的具体食物和份量建议，注意营养均衡（碳水50%/蛋白质20%/脂肪30%）
- 格式简洁，用emoji标注每餐"""

        try:
            response = call_ollama(message=prompt)
            plan = {
                "target_calories": round(target, 0),
                "ai_plan": response,
                "meals": {
                    "breakfast": {"calories": round(target * 0.30, 0), "suggestion": "燕麦+鸡蛋+牛奶"},
                    "lunch": {"calories": round(target * 0.40, 0), "suggestion": "糙米饭+鸡胸肉+蔬菜"},
                    "dinner": {"calories": round(target * 0.25, 0), "suggestion": "清蒸鱼+蔬菜+少量主食"},
                    "snack": {"calories": round(target * 0.05, 0), "suggestion": "水果或坚果"},
                }
            }
        except Exception:
            plan = {
                "target_calories": round(target, 0),
                "meals": {
                    "breakfast": {"calories": round(target * 0.30, 0), "suggestion": "燕麦+鸡蛋+牛奶"},
                    "lunch": {"calories": round(target * 0.40, 0), "suggestion": "糙米饭+鸡胸肉+蔬菜"},
                    "dinner": {"calories": round(target * 0.25, 0), "suggestion": "清蒸鱼+蔬菜+少量主食"},
                    "snack": {"calories": round(target * 0.05, 0), "suggestion": "水果或坚果"},
                }
            }
        return plan
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/health")
async def health_check():
    return {"status": "ok", "service": "DietAI AI Service", "model": OLLAMA_MODEL}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)