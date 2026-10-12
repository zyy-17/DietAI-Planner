import logging
from typing import Optional, List
from service.llm_service import call_llm_text, call_llm_stream, get_model, get_backend
from prompt.system_prompt import SYSTEM_PROMPT
from model.request import ChatHistoryItem
from config.settings import MAX_HISTORY_ROUNDS

logger = logging.getLogger("dietai")

FALLBACK_MSG = "⚠️ AI 服务暂时不可用，请稍后重试。请检查 ai-service/.env 中的 API_KEY 是否填写正确，或本机 Ollama 是否正在运行。"


def build_messages(message: str, context: Optional[str] = None,
                   history: Optional[List[ChatHistoryItem]] = None) -> list:
    system = SYSTEM_PROMPT
    if context:
        system += f"\n\n━━━ 当前用户数据 ━━━\n{context}\n━━━━━━━━━━━━━━━━━━"

    messages = [{"role": "system", "content": system}]

    if history:
        if len(history) > MAX_HISTORY_ROUNDS * 2:
            trimmed = history[-(MAX_HISTORY_ROUNDS * 2):]
            logger.info(f"历史消息截断: {len(history)}条 → {len(trimmed)}条(保留最近{MAX_HISTORY_ROUNDS}轮)")
            history = trimmed
        for item in history:
            messages.append({"role": item.role, "content": item.content})

    messages.append({"role": "user", "content": message})
    return messages


def chat(message: str, context: str, session_id: int,
         history: Optional[List[ChatHistoryItem]] = None) -> dict:
    model = get_model()
    if model == "none":
        logger.error("没有可用的 LLM：后端=%s", get_backend())
        return {"response": FALLBACK_MSG, "session_id": session_id, "fallback": True, "error": "no_model"}

    try:
        messages = build_messages(message, context, history)
        response = call_llm_text(messages)
        return {"response": response, "session_id": session_id}
    except Exception as e:
        logger.error(f"LLM 调用失败: {e}")
        return {"response": FALLBACK_MSG, "session_id": session_id, "fallback": True, "error": str(e)}


def chat_stream(message: str, context: str, session_id: int,
                history: Optional[List[ChatHistoryItem]] = None):
    messages = build_messages(message, context, history)
    return call_llm_stream(messages)


def generate_title(message: str, response: str) -> str:
    """让 LLM 为一次对话生成不超过 12 字的概括性标题。

    失败时返回空串，调用方自行兜底（如使用用户首句截断）。
    max_tokens 给 512：思维链模型会先思考再输出正式答案，太小会被 reasoning_tokens 占满。
    """
    prompt = (
        "请用不超过12个汉字概括下面这次对话的主题，只输出标题文字，"
        "不要加引号、标点、解释或任何多余字符。\n\n"
        f"用户问题：{message}\n\n"
        f"AI回复（节选）：{response[:400]}"
    )
    messages = [{"role": "user", "content": prompt}]
    try:
        title = call_llm_text(messages, max_tokens=512).strip()
        # 去掉模型可能自带的引号/书名号
        title = title.strip('\'""《》 ')
        if title and len(title) <= 24:
            return title
        return ""
    except Exception as e:
        logger.error(f"生成对话标题失败: {e}")
        return ""
