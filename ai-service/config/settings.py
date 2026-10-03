import os

OLLAMA_HOST = os.getenv("OLLAMA_HOST", "http://localhost:11434")

PREFERRED_MODELS = [
    "qwen2.5:7b",
    "qwen2.5:latest",
    "qwen2.5:3b",
    "qwen2:7b",
    "qwen:latest",
    # 兜底：通用对话模型都没装时，才退到代码模型
    # （代码模型在本项目的营养咨询场景下指令遵循较弱，且体积更大、推理更慢）
    "qwen2.5-coder:7b",
]

MODEL_OPTIONS = {
    "temperature": 0.6,
    # 上下文需容纳：系统提示 + 用户画像/今日明细/近7天逐日明细 + 最多10轮历史对话
    "num_ctx": 16384,
    "top_p": 0.9,
    "repeat_penalty": 1.1,
}

MAX_RETRIES = 5
# 单次 Ollama 请求的等待上限（秒）。本机 CPU 推理 + 长上下文时，一次回复可能需要 2-3 分钟
REQUEST_TIMEOUT = int(os.getenv("REQUEST_TIMEOUT", "300"))
MAX_HISTORY_ROUNDS = int(os.getenv("MAX_HISTORY_ROUNDS", "10"))