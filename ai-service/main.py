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

app = FastAPI(title="DietAI AI Service", version="3.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

PREFERRED_MODELS = [
    "qwen2.5:7b",
    "qwen2.5:latest",
    "qwen2.5-coder:7b",
    "qwen2:7b",
    "qwen:latest",
]

OLLAMA_MODEL = "qwen2.5:7b"


def detect_model() -> str:
    try:
        installed = ollama.list()
        installed_names = {m.model.split(":")[0]: m.model for m in installed.models}

        for pref in PREFERRED_MODELS:
            base = pref.split(":")[0]
            if base in installed_names:
                full = installed_names[base]
                if ":" in full:
                    pass
                logger.info(f"模型已检测到: {full}")
                return full
            try:
                test = ollama.show(pref)
                if test:
                    logger.info(f"模型已检测到: {pref}")
                    return pref
            except Exception:
                try:
                    logger.info(f"正在尝试拉取模型: {pref}")
                    ollama.pull(pref)
                    logger.info(f"模型拉取成功: {pref}")
                    return pref
                except Exception:
                    continue

        if installed.models:
            fallback = installed.models[0].model
            logger.warning(f"未找到推荐模型，使用第一个已安装模型: {fallback}")
            return fallback

        logger.warning("未找到任何 Ollama 模型，将使用降级回复")
        return "none"
    except Exception as e:
        logger.warning(f"检测模型失败: {e}，将使用降级回复")
        return "none"


OLLAMA_MODEL = detect_model()

MODEL_OPTIONS = {
    "temperature": 0.5,
    "num_ctx": 8192,
    "top_p": 0.85,
    "top_k": 35,
    "repeat_penalty": 1.05,
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


SYSTEM_PROMPT = """你是"智慧膳食"，一个知识渊博、热情友善的AI助手。你既能解答饮食营养问题，也能回答科学、生活、健康等各类问题。

核心原则：
- 用科学事实回答，不编造。不确定时坦诚说明
- 回答简洁实用，像朋友聊天一样自然
- 涉及医疗问题提醒用户咨询专业医生
- 适当用emoji让回复更生动

当用户提供健康数据（身高/体重/年龄/饮食目标/饮食偏好）时：
- 结合数据给出个性化膳食建议和营养分析
- 根据饮食目标（减脂/维持/增肌）推荐合适的食物和份量
- 参考营养均衡比例：碳水50%、蛋白质20%、脂肪30%
"""


def build_messages(message: str, context: Optional[str] = None,
                   history: Optional[List[ChatHistoryItem]] = None) -> list:
    system = SYSTEM_PROMPT
    if context:
        system += f"\n\n[用户健康数据]\n{context}"

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


def generate_fallback_response(message: str) -> str:
    msg = message.lower()

    if any(w in msg for w in ["吃什么", "推荐", "食谱", "菜单", "搭配"]):
        return """🍽️ 推荐均衡膳食搭配：
• 主食：糙米饭/全麦面包 100-150g
• 蛋白质：鸡胸肉150g / 鱼200g / 豆腐200g
• 蔬菜：深色蔬菜 200-300g
• 脂肪：少量坚果或橄榄油
💡 先吃蔬菜→再吃蛋白质→最后吃主食，有助控制血糖"""

    if any(w in msg for w in ["热量", "卡路里", "kcal"]):
        return """📊 每日热量：
• 减脂：TDEE × 0.8（300-500kcal缺口）
• 维持：TDEE × 1.0
• 增肌：TDEE × 1.15
在"今日饮食"页面记录摄入，系统自动计算剩余"""

    if any(w in msg for w in ["减脂", "减肥", "瘦"]):
        return """🎯 减脂要点：
• 日热量缺口 300-500kcal
• 高蛋白（1.6-2.0g/kg）保护肌肉
• 优先低GI碳水（燕麦/糙米/红薯）
• 每周3-5次有氧+力量训练
⚠️ 不要节食！营养均衡才可持续"""

    if any(w in msg for w in ["增肌", "肌肉"]):
        return """💪 增肌要点：
• 日热量盈余 200-400kcal
• 蛋白质 1.6-2.2g/kg体重
• 训练后30分钟内补充蛋白+碳水
• 每周4-6次力量训练，每次45-75分钟"""

    if any(w in msg for w in ["运动", "健身", "锻炼"]):
        return """🏃 运动建议：
• 减脂：有氧为主+力量辅助，每周3-5次
• 增肌：力量为主+适量有氧，每周4-6次
• 每次热身10分钟+拉伸10分钟
• 循序渐进，避免过度训练"""

    if any(w in msg for w in ["睡眠", "失眠", "熬夜"]):
        return """😴 科学睡眠：
• 成人每天7-9小时
• 固定作息，周末不偏差超1小时
• 睡前1小时远离手机/电脑蓝光
• 卧室18-22°C，安静黑暗
• 睡前3小时完成晚餐"""

    if any(w in msg for w in ["压力", "焦虑", "emo", "心情不好"]):
        return """🧘 减压建议：
• 规律运动是最有效的天然抗压方式
• 深呼吸：4秒吸→7秒屏→8秒呼
• 正念冥想：每天10分钟
• 充足睡眠是情绪稳定的基础
• 需要时寻求心理咨询是智慧，不是软弱"""

    return """你好！我是智慧膳食 🍎

我可以帮你：
• 🍽️ 膳食计划、营养分析、食物热量查询
• 🏃 运动健身、训练计划
• 😴 睡眠、减压、健康习惯
• 📚 科学知识问答

请告诉我你想了解什么？"""


def _handle_chat(message: str, context: str, session_id: int, history: list, stream: bool):
    if OLLAMA_MODEL == "none":
        fallback = generate_fallback_response(message)
        return {"response": fallback, "session_id": session_id, "fallback": True, "error": "no_model"}

    try:
        if stream:
            return _stream_chat(message, context, session_id, history)
        else:
            response = call_ollama(message=message, context=context, history=history)
            return {"response": response, "session_id": session_id}
    except Exception as e:
        logger.error(f"Ollama 调用失败: {e}")
        fallback = generate_fallback_response(message)
        return {"response": fallback, "session_id": session_id, "fallback": True, "error": str(e)}


def _stream_chat(message: str, context: str, session_id: int, history: list):
    def event_generator():
        try:
            for chunk in call_ollama_stream(message=message, context=context, history=history):
                yield f"data: {json.dumps({'content': chunk}, ensure_ascii=False)}\n\n"
            yield f"data: {json.dumps({'done': True, 'session_id': session_id}, ensure_ascii=False)}\n\n"
        except Exception as e:
            logger.error(f"流式调用失败: {e}")
            fallback = generate_fallback_response(message)
            yield f"data: {json.dumps({'content': fallback, 'fallback': True, 'error': str(e)}, ensure_ascii=False)}\n\n"
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

        prompt = (
            f"生成一日膳食计划：目标{round(target)}kcal，目标为{goal_desc}。"
            f"给出早中晚三餐+加餐的具体食物、克数、热量。碳水50%/蛋白质20%/脂肪30%。简洁emoji格式。"
        )

        plan = {
            "target_calories": round(target, 0),
            "meals": {
                "breakfast": {"calories": round(target * 0.30, 0), "suggestion": "燕麦+鸡蛋+牛奶"},
                "lunch": {"calories": round(target * 0.40, 0), "suggestion": "糙米饭+鸡胸肉+蔬菜"},
                "dinner": {"calories": round(target * 0.25, 0), "suggestion": "清蒸鱼+蔬菜+少量主食"},
                "snack": {"calories": round(target * 0.05, 0), "suggestion": "水果或坚果"},
            }
        }

        if OLLAMA_MODEL != "none":
            try:
                ai_plan = call_ollama(message=prompt)
                plan["ai_plan"] = ai_plan
            except Exception as e:
                logger.error(f"膳食规划AI生成失败: {e}")

        return plan
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/health")
async def health_check():
    return {
        "status": "ok",
        "service": "DietAI AI Service",
        "version": "3.0.0",
        "model": OLLAMA_MODEL,
    }


if __name__ == "__main__":
    logger.info(f"启动 AI 服务，模型: {OLLAMA_MODEL}")
    logger.info(f"模型参数: temperature={MODEL_OPTIONS['temperature']}, num_ctx={MODEL_OPTIONS['num_ctx']}")
    uvicorn.run(app, host="0.0.0.0", port=8000)