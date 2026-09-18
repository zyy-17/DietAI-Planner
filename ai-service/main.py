from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from typing import Optional, List
import ollama
from ollama import chat as ollama_chat
from ollama import ChatResponse
import uvicorn
import json
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("dietai")

app = FastAPI(title="DietAI AI Service", version="4.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

PREFERRED_MODELS = [
    "qwen2.5-coder:7b",
    "qwen2.5:7b",
    "qwen2.5:latest",
    "qwen2:7b",
    "qwen:latest",
]

OLLAMA_MODEL = "qwen2.5-coder:7b"


def detect_model() -> str:
    try:
        installed = ollama.list()
        installed_names = {m.model.split(":")[0]: m.model for m in installed.models}

        for pref in PREFERRED_MODELS:
            base = pref.split(":")[0]
            if base in installed_names:
                full = installed_names[base]
                logger.info(f"模型已检测到: {full}")
                return full
            try:
                test = ollama.show(pref)
                if test:
                    logger.info(f"模型已检测到: {pref}")
                    return pref
            except Exception:
                continue

        if installed.models:
            fallback = installed.models[0].model
            logger.warning(f"未找到推荐模型，使用第一个已安装模型: {fallback}")
            return fallback

        logger.warning("未找到任何 Ollama 模型")
        return "none"
    except Exception as e:
        logger.warning(f"检测模型失败: {e}")
        return "none"


OLLAMA_MODEL = detect_model()

MODEL_OPTIONS = {
    "temperature": 0.6,
    "num_ctx": 8192,
    "top_p": 0.9,
    "repeat_penalty": 1.1,
}


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


SYSTEM_PROMPT = """你是"智慧膳食"，一个专业的AI营养健康助手。

你的核心职责：
1. 理解用户的自然语言问题，给出科学、准确的回答
2. 当对话中提供了用户个人数据时，基于数据给出个性化建议
3. 保持与用户的连续对话上下文，记住之前的交流内容
4. 给出具体可执行的建议，而非空泛理论
5. 涉及医疗诊断时，提醒用户咨询专业医生

【如何利用用户数据】
当上下文中包含用户数据时，请这样使用：
- 用户画像（姓名/性别/年龄/身高/体重/活动水平）：据此判断用户的基础代谢和营养需求
- 饮食目标（减脂/维持/增肌）：据此调整热量和营养素建议
- 饮食偏好：推荐食物时严格遵守偏好，如清淡/素食/低糖等
- 今日已摄入数据：分析当日营养是否达标，指出缺口
- 剩余热量和营养素：据此推荐下一餐或加餐的具体食物和份量
- 如果没有提供数据，就按通用知识回答，不要编造用户信息

【回答原则】
- 有数据时做个性化分析，没数据时做通用科普
- 不要机械重复固定模板，每次根据实际情况灵活回答
- 用户问什么就答什么，不要强行把所有问题都引向饮食话题
- 可以正常回答科学、数学、编程、历史等各类问题
- 适当用emoji让回复更生动，但不要过度
- 回答简洁但内容充实，像一位知识渊博的朋友
"""


def build_messages(message: str, context: Optional[str] = None,
                   history: Optional[List[ChatHistoryItem]] = None) -> list:
    system = SYSTEM_PROMPT
    if context:
        system += f"\n\n━━━ 当前用户数据 ━━━\n{context}\n━━━━━━━━━━━━━━━━━━"

    messages = [{"role": "system", "content": system}]

    if history:
        for item in history:
            messages.append({"role": item.role, "content": item.content})

    messages.append({"role": "user", "content": message})
    return messages


def _call_ollama_internal(messages: list, stream: bool = False):
    if OLLAMA_MODEL == "none":
        raise RuntimeError("no_model")
    return ollama_chat(
        model=OLLAMA_MODEL,
        messages=messages,
        stream=stream,
        options=MODEL_OPTIONS,
    )


def call_ollama(message: str, context: Optional[str] = None,
                history: Optional[List[ChatHistoryItem]] = None) -> str:
    messages = build_messages(message, context, history)
    response: ChatResponse = _call_ollama_internal(messages, stream=False)
    return response.message.content


def call_ollama_stream(message: str, context: Optional[str] = None,
                       history: Optional[List[ChatHistoryItem]] = None):
    messages = build_messages(message, context, history)
    response = _call_ollama_internal(messages, stream=True)
    for chunk in response:
        if chunk.message.content is not None:
            yield chunk.message.content


FALLBACK_MSG = "⚠️ AI 服务暂时不可用，请稍后重试。您可以检查 Ollama 是否正在运行，以及模型是否已正确安装。"


def _handle_chat(message: str, context: str, session_id: int, history: list, stream: bool):
    if OLLAMA_MODEL == "none":
        return {"response": FALLBACK_MSG, "session_id": session_id, "fallback": True, "error": "no_model"}

    try:
        if stream:
            return _stream_chat(message, context, session_id, history)
        else:
            response = call_ollama(message=message, context=context, history=history)
            return {"response": response, "session_id": session_id}
    except Exception as e:
        logger.error(f"Ollama 调用失败: {e}")
        return {"response": FALLBACK_MSG, "session_id": session_id, "fallback": True, "error": str(e)}


def _stream_chat(message: str, context: str, session_id: int, history: list):
    def event_generator():
        try:
            for chunk in call_ollama_stream(message=message, context=context, history=history):
                yield f"data: {json.dumps({'content': chunk}, ensure_ascii=False)}\n\n"
            yield f"data: {json.dumps({'done': True, 'session_id': session_id}, ensure_ascii=False)}\n\n"
        except Exception as e:
            logger.error(f"流式调用失败: {e}")
            yield f"data: {json.dumps({'content': FALLBACK_MSG, 'fallback': True, 'error': str(e)}, ensure_ascii=False)}\n\n"
            yield f"data: {json.dumps({'done': True, 'session_id': session_id}, ensure_ascii=False)}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


@app.post("/api/chat")
async def chat(chat_message: ChatMessage):
    return _handle_chat(
        message=chat_message.message,
        context=chat_message.context or "",
        session_id=chat_message.session_id or 0,
        history=None,
        stream=False,
    )


@app.post("/api/chat/history")
async def chat_with_history(request: ChatWithHistoryRequest):
    return _handle_chat(
        message=request.message,
        context=request.context or "",
        session_id=request.session_id or 0,
        history=request.history,
        stream=False,
    )


@app.post("/api/chat/stream")
async def chat_stream(chat_message: ChatMessage):
    return _handle_chat(
        message=chat_message.message,
        context=chat_message.context or "",
        session_id=chat_message.session_id or 0,
        history=None,
        stream=True,
    )


@app.post("/api/chat/history/stream")
async def chat_with_history_stream(request: ChatWithHistoryRequest):
    return _handle_chat(
        message=request.message,
        context=request.context or "",
        session_id=request.session_id or 0,
        history=request.history,
        stream=True,
    )


@app.post("/api/diet-plan")
async def generate_diet_plan(request: DietPlanRequest):
    try:
        target = request.target_calories or 2000
        goal = request.diet_goal or "maintain"
        goal_desc = {"lose": "减脂", "maintain": "维持体重", "gain": "增肌"}.get(goal, "维持体重")

        if goal == "lose":
            target *= 0.8
        elif goal == "gain":
            target *= 1.15

        user_context = request.context or "无额外用户信息"

        prompt = (
            f"请根据以下信息生成一份个性化的一日膳食计划：\n"
            f"【目标热量】{round(target)} kcal\n"
            f"【饮食目标】{goal_desc}\n"
            f"【用户信息】{user_context}\n"
            f"【要求】\n"
            f"1. 给出早餐、午餐、晚餐、加餐的具体食物和克数\n"
            f"2. 每餐标注大约热量\n"
            f"3. 营养配比：碳水50%、蛋白质20%、脂肪30%\n"
            f"4. 如果用户有饮食偏好或忌口，严格遵守\n"
            f"5. 用emoji标注每餐，格式简洁\n"
        )

        plan = {
            "target_calories": round(target, 0),
            "meals": {
                "breakfast": {"calories": round(target * 0.30, 0), "suggestion": ""},
                "lunch": {"calories": round(target * 0.40, 0), "suggestion": ""},
                "dinner": {"calories": round(target * 0.25, 0), "suggestion": ""},
                "snack": {"calories": round(target * 0.05, 0), "suggestion": ""},
            }
        }

        if OLLAMA_MODEL != "none":
            try:
                ai_plan = call_ollama(message=prompt)
                plan["ai_plan"] = ai_plan
            except Exception as e:
                logger.error(f"膳食规划AI生成失败: {e}")
                plan["ai_plan"] = "AI 服务暂不可用，请稍后重试。"

        return plan
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/health")
async def health_check():
    return {
        "status": "ok",
        "service": "DietAI AI Service",
        "version": "4.0.0",
        "model": OLLAMA_MODEL,
    }


if __name__ == "__main__":
    logger.info(f"启动 AI 服务 v4.0，模型: {OLLAMA_MODEL}")
    uvicorn.run(app, host="0.0.0.0", port=8000)