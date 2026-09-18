from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from typing import Optional, List
from ollama import chat as ollama_chat
from ollama import ChatResponse
import uvicorn
import json

app = FastAPI(title="DietAI AI Service", version="2.0.0")

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


SYSTEM_PROMPT = """你是一个名为"智慧膳食"的AI全能健康顾问。你同时具备以下两大核心能力：

═══════════════════════════════════════
【核心能力一：专业膳食营养顾问】
═══════════════════════════════════════
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
- 推荐食物时考虑营养均衡（碳水50%、蛋白质20%、脂肪30%）

═══════════════════════════════════════
【核心能力二：科学知识百科顾问】
═══════════════════════════════════════
当用户询问非膳食领域的问题时，你同样能够给出科学、准确、有深度的回答：

1. **健康医学**：常见疾病预防、健康生活习惯、心理健康、睡眠科学、运动科学
2. **运动健身**：训练计划制定、运动损伤预防、不同目标的训练方法、运动营养配合
3. **生活常识**：日常健康小知识、食品安全、食材选购与保存、烹饪科学
4. **科学知识**：生物学、化学、物理学等与健康生活相关的科学原理
5. **心理情绪**：压力管理、情绪调节、习惯养成、自律方法
6. **其他领域**：数学、编程、语言、历史等通用知识，尽力给出准确回答

回答原则：
- 始终基于科学事实和权威知识，不编造信息
- 涉及医疗诊断时，提醒用户咨询专业医生
- 给出实用可操作的建议，而非空泛理论
- 适当引用数据、研究或权威来源增加可信度

═══════════════════════════════════════
【通用回复规范】
═══════════════════════════════════════
1. 每次只回1条消息，内容完整有条理
2. 回复简洁实用，像一位博学的朋友在聊天
3. 合适时用🍎🥗💪🍽️📊🔥💡⚠️✅等emoji让回复更生动
4. 用亲切自然的语气，称呼用户时如果知道姓名就用姓名
5. 匹配用户的语言风格（用户简短则简短回复，用户详细则详细回复）
6. 涉及数字和具体建议时，给出明确数值，避免模糊表述
7. 如果不确定某个信息，坦诚说明并给出已知的最可靠建议
"""


def build_system_prompt(context: Optional[str] = None) -> str:
    prompt = SYSTEM_PROMPT
    if context:
        prompt += f"\n\n当前用户健康数据：\n{context}\n"
    return prompt


def build_messages(message: str, context: Optional[str] = None,
                   history: Optional[List[ChatHistoryItem]] = None) -> list:
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

    return messages


def call_ollama(message: str, context: Optional[str] = None,
                history: Optional[List[ChatHistoryItem]] = None) -> str:
    messages = build_messages(message, context, history)

    response: ChatResponse = ollama_chat(
        model=OLLAMA_MODEL,
        messages=messages,
        stream=False,
        options={
            'temperature': 0.7,
            'num_ctx': 4096,
            'top_p': 0.9,
            'top_k': 40,
        }
    )

    return response.message.content


def call_ollama_stream(message: str, context: Optional[str] = None,
                       history: Optional[List[ChatHistoryItem]] = None):
    messages = build_messages(message, context, history)

    response = ollama_chat(
        model=OLLAMA_MODEL,
        messages=messages,
        stream=True,
        options={
            'temperature': 0.7,
            'num_ctx': 4096,
            'top_p': 0.9,
            'top_k': 40,
        }
    )

    for chunk in response:
        if chunk.message.content is not None:
            yield chunk.message.content


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

    elif "运动" in message or "健身" in message or "锻炼" in message:
        return """🏃 运动建议：
• 减脂：有氧为主（跑步/游泳/骑车）+ 力量训练辅助，每周3-5次
• 增肌：力量训练为主 + 适量有氧，每周4-6次
• 维持：混合训练，每周3-4次
💡 运动前热身10分钟，运动后拉伸10分钟，预防损伤。"""

    elif "睡眠" in message or "失眠" in message or "作息" in message:
        return """😴 科学睡眠建议：
• 成人每日7-9小时睡眠
• 固定作息时间，周末偏差不超过1小时
• 睡前1小时避免蓝光（手机/电脑）
• 卧室温度18-22°C，保持安静黑暗
• 晚餐不宜过饱，睡前3小时完成进食"""

    elif "压力" in message or "焦虑" in message or "心情" in message:
        return """🧘 压力管理建议：
• 规律运动是最有效的天然减压方式
• 深呼吸练习：4秒吸气→7秒屏息→8秒呼气
• 正念冥想：每天10分钟，专注当下
• 充足睡眠是情绪稳定的基础
• 必要时寻求专业心理咨询，这不是软弱而是智慧"""

    else:
        return """您好！我是您的智能健康顾问 🍎

我可以帮您：
• 🍽️ 制定膳食计划、分析营养摄入、查询食物热量
• 🏃 运动健身建议、训练计划制定
• 😴 睡眠、压力管理、健康生活习惯
• 📚 回答各类科学知识问题

请告诉我您想了解什么？"""


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


@app.post("/api/chat/stream")
async def chat_stream(chat_message: ChatMessage):
    def event_generator():
        try:
            for chunk in call_ollama_stream(
                message=chat_message.message,
                context=chat_message.context
            ):
                yield f"data: {json.dumps({'content': chunk}, ensure_ascii=False)}\n\n"
            yield f"data: {json.dumps({'done': True, 'session_id': chat_message.session_id}, ensure_ascii=False)}\n\n"
        except Exception as e:
            fallback = generate_fallback_response(chat_message.message, chat_message.context)
            yield f"data: {json.dumps({'content': fallback, 'fallback': True, 'error': str(e)}, ensure_ascii=False)}\n\n"
            yield f"data: {json.dumps({'done': True, 'session_id': chat_message.session_id}, ensure_ascii=False)}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


@app.post("/api/chat/history/stream")
async def chat_with_history_stream(request: ChatWithHistoryRequest):
    def event_generator():
        try:
            for chunk in call_ollama_stream(
                message=request.message,
                context=request.context,
                history=request.history
            ):
                yield f"data: {json.dumps({'content': chunk}, ensure_ascii=False)}\n\n"
            yield f"data: {json.dumps({'done': True, 'session_id': request.session_id}, ensure_ascii=False)}\n\n"
        except Exception as e:
            fallback = generate_fallback_response(request.message, request.context)
            yield f"data: {json.dumps({'content': fallback, 'fallback': True, 'error': str(e)}, ensure_ascii=False)}\n\n"
            yield f"data: {json.dumps({'done': True, 'session_id': request.session_id}, ensure_ascii=False)}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


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
- 格式简洁，用emoji标注每餐
- 每餐给出具体克数和对应热量"""

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
    return {"status": "ok", "service": "DietAI AI Service", "version": "2.0.0", "model": OLLAMA_MODEL}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)