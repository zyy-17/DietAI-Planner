from fastapi import APIRouter
from service.llm_service import get_model

router = APIRouter()


@router.get("/api/health")
async def health_check():
    return {
        "status": "ok",
        "service": "DietAI AI Service",
        "version": "5.0.0",
        "model": get_model(),
    }