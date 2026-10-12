from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
import json
import logging
from model.request import ChatMessage, ChatWithHistoryRequest, TitleGenerateRequest
from service.chat_service import chat, chat_stream, generate_title, FALLBACK_MSG

logger = logging.getLogger("dietai")
router = APIRouter()


@router.post("/api/chat")
async def chat_endpoint(chat_message: ChatMessage):
    return chat(
        message=chat_message.message,
        context=chat_message.context or "",
        session_id=chat_message.session_id or 0,
        history=None,
    )


@router.post("/api/chat/history")
async def chat_with_history_endpoint(request: ChatWithHistoryRequest):
    return chat(
        message=request.message,
        context=request.context or "",
        session_id=request.session_id or 0,
        history=request.history,
    )


@router.post("/api/chat/stream")
async def chat_stream_endpoint(chat_message: ChatWithHistoryRequest):
    messages = chat_stream(
        message=chat_message.message,
        context=chat_message.context or "",
        session_id=chat_message.session_id or 0,
        history=chat_message.history,
    )
    session_id = chat_message.session_id or 0

    def event_generator():
        try:
            for chunk in messages:
                yield f"data: {json.dumps({'content': chunk}, ensure_ascii=False)}\n\n"
            yield f"data: {json.dumps({'done': True, 'session_id': session_id}, ensure_ascii=False)}\n\n"
        except Exception as e:
            logger.error(f"流式调用失败: {e}")
            yield f"data: {json.dumps({'content': FALLBACK_MSG, 'fallback': True, 'error': str(e)}, ensure_ascii=False)}\n\n"
            yield f"data: {json.dumps({'done': True, 'session_id': session_id}, ensure_ascii=False)}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


@router.post("/api/chat/history/stream")
async def chat_with_history_stream_endpoint(request: ChatWithHistoryRequest):
    messages = chat_stream(
        message=request.message,
        context=request.context or "",
        session_id=request.session_id or 0,
        history=request.history,
    )
    session_id = request.session_id or 0

    def event_generator():
        try:
            for chunk in messages:
                yield f"data: {json.dumps({'content': chunk}, ensure_ascii=False)}\n\n"
            yield f"data: {json.dumps({'done': True, 'session_id': session_id}, ensure_ascii=False)}\n\n"
        except Exception as e:
            logger.error(f"流式调用失败: {e}")
            yield f"data: {json.dumps({'content': FALLBACK_MSG, 'fallback': True, 'error': str(e)}, ensure_ascii=False)}\n\n"
            yield f"data: {json.dumps({'done': True, 'session_id': session_id}, ensure_ascii=False)}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


@router.post("/api/chat/title")
async def generate_title_endpoint(request: TitleGenerateRequest):
    """为一次对话生成不超过 12 字的概括性标题。"""
    title = generate_title(message=request.message, response=request.response)
    return {"title": title}