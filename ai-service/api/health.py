from fastapi import APIRouter
from service.llm_service import get_model, get_backend
from config.settings import API_BASE_URL, API_MODEL

router = APIRouter()


@router.get("/api/health")
async def health_check():
    backend = get_backend()
    return {
        "status": "ok",
        "service": "DietAI AI Service",
        "version": "5.1.0",
        "backend": backend,
        "model": get_model(),
        "api_base_url": API_BASE_URL if backend == "api" else None,
        "api_model": API_MODEL if backend == "api" else None,
    }
