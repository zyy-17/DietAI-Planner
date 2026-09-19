import ollama
from ollama import chat as ollama_chat
from ollama import ChatResponse as OllamaChatResponse
import logging
from config.settings import PREFERRED_MODELS, MODEL_OPTIONS

logger = logging.getLogger("dietai")

_OLLAMA_MODEL = None


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


def get_model() -> str:
    global _OLLAMA_MODEL
    if _OLLAMA_MODEL is None:
        _OLLAMA_MODEL = detect_model()
    return _OLLAMA_MODEL


def call_ollama(messages: list, stream: bool = False, fmt: str = None):
    model = get_model()
    if model == "none":
        raise RuntimeError("no_model")
    kwargs = dict(
        model=model,
        messages=messages,
        stream=stream,
        options=MODEL_OPTIONS,
    )
    if fmt:
        kwargs["format"] = fmt
    return ollama_chat(**kwargs)


def call_ollama_text(messages: list, fmt: str = None) -> str:
    response: OllamaChatResponse = call_ollama(messages, stream=False, fmt=fmt)
    return response.message.content


def call_ollama_stream(messages: list):
    response = call_ollama(messages, stream=True)
    for chunk in response:
        if chunk.message.content is not None:
            yield chunk.message.content