from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("dietai")

app = FastAPI(title="DietAI AI Service", version="5.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

from api.chat import router as chat_router
from api.diet_plan import router as diet_plan_router
from api.health import router as health_router

app.include_router(chat_router)
app.include_router(diet_plan_router)
app.include_router(health_router)


if __name__ == "__main__":
    from service.llm_service import get_model, get_backend
    from config.settings import (
        API_BASE_URL,
        API_KEY,
        AI_SERVICE_HOST,
        AI_SERVICE_PORT,
    )

    backend = get_backend()
    model = get_model()
    if backend == "api":
        if API_KEY:
            logger.info(f"启动 AI 服务 v5.1 | 后端=云端API | 接口={API_BASE_URL} | 模型={model}")
        else:
            logger.error("启动 AI 服务 v5.1 | 后端=云端API | ❌ 未检测到 API_KEY，请检查 ai-service/.env")
    else:
        logger.info(f"启动 AI 服务 v5.1 | 后端=本机Ollama | 模型={model}")

    logger.info(f"监听地址 http://{AI_SERVICE_HOST}:{AI_SERVICE_PORT}")
    uvicorn.run(app, host=AI_SERVICE_HOST, port=AI_SERVICE_PORT)
