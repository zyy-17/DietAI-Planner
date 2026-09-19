import logging
from typing import Optional, List
from service.llm_service import call_ollama_text, call_ollama_stream, get_model
from prompt.system_prompt import SYSTEM_PROMPT
from model.request import ChatHistoryItem

logger = logging.getLogger("dietai")

FALLBACK_MSG = "⚠️ AI 服务暂时不可用，请稍后重试。您可以检查 Ollama 是否正在运行，以及模型是否已正确安装。"


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


def chat(message: str, context: str, session_id: int,
         history: Optional[List[ChatHistoryItem]] = None) -> dict:
    if get_model() == "none":
        return {"response": FALLBACK_MSG, "session_id": session_id, "fallback": True, "error": "no_model"}

    try:
        messages = build_messages(message, context, history)
        response = call_ollama_text(messages)
        return {"response": response, "session_id": session_id}
    except Exception as e:
        logger.error(f"Ollama 调用失败: {e}")
        return {"response": FALLBACK_MSG, "session_id": session_id, "fallback": True, "error": str(e)}


def chat_stream(message: str, context: str, session_id: int,
                history: Optional[List[ChatHistoryItem]] = None):
    messages = build_messages(message, context, history)
    return call_ollama_stream(messages)