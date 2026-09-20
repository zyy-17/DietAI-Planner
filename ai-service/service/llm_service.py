import ollama
from ollama import chat as ollama_chat
from ollama import ChatResponse as OllamaChatResponse
import logging
import time
from config.settings import PREFERRED_MODELS, MODEL_OPTIONS, REQUEST_TIMEOUT

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


def call_ollama_text(messages: list, fmt: str = None, retries: int = 3) -> str:
    last_error = None
    for attempt in range(retries):
        try:
            start = time.time()
            response: OllamaChatResponse = call_ollama(messages, stream=False, fmt=fmt)
            content = response.message.content
            elapsed = time.time() - start
            logger.info(f"Ollama调用完成 耗时={elapsed:.1f}s 尝试={attempt+1}/{retries} 输出长度={len(content) if content else 0}")
            if content and content.strip():
                return content
            logger.warning(f"Ollama返回空内容(尝试{attempt+1})")
        except Exception as e:
            elapsed = time.time() - start if 'start' in dir() else 0
            last_error = e
            logger.warning(f"Ollama调用失败(尝试{attempt+1}/{retries}): {e}")
            if attempt < retries - 1:
                time.sleep(2)
    raise RuntimeError(f"Ollama调用{retries}次均失败: {last_error}")


def call_ollama_stream(messages: list):
    response = call_ollama(messages, stream=True)
    for chunk in response:
        if chunk.message.content is not None:
            yield chunk.message.content