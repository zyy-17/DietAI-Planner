import json
import logging
import re
import time
from typing import Optional

import httpx

from config.settings import (
    API_BASE_URL,
    API_KEY,
    API_MAX_TOKENS,
    API_MODEL,
    API_TEMPERATURE,
    API_TIMEOUT,
    MODEL_OPTIONS,
    PREFERRED_MODELS,
    REQUEST_TIMEOUT,
    resolved_backend,
)

logger = logging.getLogger("dietai")

_OLLAMA_MODEL = None


# ══════════════════════════════════════════════════════════════════
#  Ollama 后端
# ══════════════════════════════════════════════════════════════════
def _detect_ollama_model() -> str:
    try:
        import ollama

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
        logger.warning(f"检测 Ollama 模型失败: {e}")
        return "none"


def _call_ollama(messages: list, stream: bool = False, fmt: Optional[str] = None):
    from ollama import chat as ollama_chat

    kwargs = dict(
        model=get_model(),
        messages=messages,
        stream=stream,
        options=MODEL_OPTIONS,
    )
    if fmt:
        kwargs["format"] = fmt
    return ollama_chat(**kwargs)


def _call_ollama_text(messages: list, fmt: Optional[str] = None) -> str:
    response = _call_ollama(messages, stream=False, fmt=fmt)
    return response.message.content or ""


def _call_ollama_stream(messages: list):
    response = _call_ollama(messages, stream=True)
    for chunk in response:
        if chunk.message.content is not None:
            yield chunk.message.content


# ══════════════════════════════════════════════════════════════════
#  云端 API 后端（OpenAI 兼容：豆包 / DeepSeek / 通义 / Kimi / 智谱 …）
# ══════════════════════════════════════════════════════════════════
def _api_headers() -> dict:
    return {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json",
    }


def _api_endpoint() -> str:
    return f"{API_BASE_URL}/chat/completions"


def _build_api_payload(
    messages: list,
    stream: bool = False,
    fmt: Optional[str] = None,
    max_tokens: Optional[int] = None,
) -> dict:
    payload = {
        "model": API_MODEL,
        "messages": messages,
        "temperature": API_TEMPERATURE,
        # max_tokens 只是上限，不写满不额外计费；调用方可以按任务复杂度单独放宽
        "max_tokens": max_tokens or API_MAX_TOKENS,
        "stream": stream,
    }
    if fmt == "json":
        payload["response_format"] = {"type": "json_object"}
    return payload


def _redact(text: str) -> str:
    """
    脱敏：把可能出现在第三方返回内容里的密钥擦掉。
    云端厂商正常不会回显 Authorization，但错误体是不可控的外部输入，
    多一层保险，避免 key 通过错误提示流进日志或数据库。
    """
    if not text:
        return text
    if API_KEY and len(API_KEY) >= 8:
        text = text.replace(API_KEY, "***REDACTED***")
    # 兜底擦掉 sk-xxxx 形态的令牌
    return re.sub(r"sk-[A-Za-z0-9_\-]{8,}", "***REDACTED***", text)


def _api_error_hint(status_code: int, body: str) -> str:
    """把常见 HTTP 错误翻译成人话，方便用户自己排查 key / 模型名填错。"""
    if status_code == 401:
        return "认证失败(401)：API_KEY 填错了或已失效，请检查 ai-service/.env 里的 API_KEY"
    if status_code == 403:
        return "无权限(403)：这个 key 没有开通该模型，或未完成实名/开通流程"
    if status_code == 404:
        return f"接口或模型不存在(404)：检查 API_BASE_URL({API_BASE_URL}) 与 API_MODEL({API_MODEL}) 是否匹配"
    if status_code == 429:
        return "请求过于频繁或额度不足(429)：稍后重试，或检查账户余额"
    if status_code >= 500:
        return f"服务端错误({status_code})：云端服务暂时不可用，稍后重试"
    return f"HTTP {status_code}: {_redact(body)[:300]}"


def _call_api_text(messages: list, fmt: Optional[str] = None, max_tokens: Optional[int] = None) -> str:
    payload = _build_api_payload(messages, stream=False, fmt=fmt, max_tokens=max_tokens)
    with httpx.Client(timeout=API_TIMEOUT) as client:
        resp = client.post(_api_endpoint(), headers=_api_headers(), json=payload)
    if resp.status_code != 200:
        raise RuntimeError(_api_error_hint(resp.status_code, resp.text))

    data = resp.json()
    choices = data.get("choices") or []
    if not choices:
        raise RuntimeError(f"云端返回内容为空: {str(data)[:300]}")

    choice = choices[0]
    message = choice.get("message") or {}
    content = message.get("content") or ""

    # 思维链模型（deepseek-reasoner 等）会先输出 thinking，再输出正式内容。
    # 思考过程把 max_tokens 吃满时 finish_reason=length、content 为空，
    # 这时必须明确报「被截断」，否则上层只会看到「空内容」而查不出原因。
    if not content.strip() and choice.get("finish_reason") == "length":
        usage = data.get("usage") or {}
        detail = usage.get("completion_tokens_details") or {}
        raise RuntimeError(
            f"输出被 max_tokens 截断：本次 {payload['max_tokens']} token 预算被思维链占满"
            f"（completion={usage.get('completion_tokens')}, "
            f"reasoning={detail.get('reasoning_tokens')}），正式内容为空。"
            f"请调大 max_tokens 或改用非思维链模型"
        )
    return content


def _call_api_stream(messages: list):
    payload = _build_api_payload(messages, stream=True)
    with httpx.Client(timeout=API_TIMEOUT) as client:
        with client.stream("POST", _api_endpoint(), headers=_api_headers(), json=payload) as resp:
            if resp.status_code != 200:
                resp.read()
                raise RuntimeError(_api_error_hint(resp.status_code, resp.text))
            for line in resp.iter_lines():
                if not line:
                    continue
                if line.startswith("data: "):
                    line = line[6:]
                if line.strip() == "[DONE]":
                    break
                try:
                    chunk = json.loads(line)
                except json.JSONDecodeError:
                    continue
                choices = chunk.get("choices") or []
                if not choices:
                    continue
                delta = choices[0].get("delta") or {}
                content = delta.get("content")
                if content:
                    yield content


# ══════════════════════════════════════════════════════════════════
#  对外统一出口：业务层只认这几个函数，不关心底层是 API 还是 Ollama
# ══════════════════════════════════════════════════════════════════
def get_backend() -> str:
    return resolved_backend()


def get_model() -> str:
    """
    返回当前后端对应的模型标识。
    - api 后端：直接返回 API_MODEL；未配置 key 时返回 'none'
    - ollama 后端：探测本机已安装模型；没有可用模型时返回 'none'
    """
    if get_backend() == "api":
        return API_MODEL if API_KEY else "none"

    global _OLLAMA_MODEL
    if _OLLAMA_MODEL is None:
        _OLLAMA_MODEL = _detect_ollama_model()
    return _OLLAMA_MODEL


def call_llm_text(
    messages: list,
    fmt: Optional[str] = None,
    retries: int = 3,
    max_tokens: Optional[int] = None,
) -> str:
    """统一的「非流式」调用，带重试。业务层用它即可。

    max_tokens 不传则用 API_MAX_TOKENS；需要长输出的任务可单独放宽
    （思维链模型的思考过程同样占用这个预算）。
    """
    last_error = None
    for attempt in range(retries):
        start = time.time()
        try:
            if get_backend() == "api":
                content = _call_api_text(messages, fmt=fmt, max_tokens=max_tokens)
            else:
                content = _call_ollama_text(messages, fmt=fmt)
            elapsed = time.time() - start
            logger.info(
                f"LLM调用完成 后端={get_backend()} 耗时={elapsed:.1f}s "
                f"尝试={attempt + 1}/{retries} 输出长度={len(content) if content else 0}"
            )
            if content and content.strip():
                return content
            logger.warning(f"LLM返回空内容(尝试{attempt + 1})")
            last_error = RuntimeError("empty_response")
        except Exception as e:
            last_error = e
            logger.warning(f"LLM调用失败(尝试{attempt + 1}/{retries}): {e}")
            # key / 模型名这类配置错误重试没有意义，直接抛出；
            # 被 max_tokens 截断同样重试无用——预算不变，再问一次还是会重蹈覆辙
            if isinstance(e, RuntimeError) and any(
                tag in str(e) for tag in ("401", "403", "404", "被 max_tokens 截断")
            ):
                break
            if attempt < retries - 1:
                time.sleep(2)
    raise RuntimeError(f"LLM调用{retries}次均失败: {last_error}")


def call_llm_stream(messages: list):
    """统一的「流式」调用。"""
    if get_backend() == "api":
        yield from _call_api_stream(messages)
    else:
        yield from _call_ollama_stream(messages)
