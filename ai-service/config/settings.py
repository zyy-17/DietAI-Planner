import os
from pathlib import Path


def _load_dotenv():
    """
    极简 .env 加载器（不引入 python-dotenv 依赖）。
    在 ai-service/.env 里写 KEY=VALUE 即可，支持 # 注释与引号包裹。
    已存在的真实环境变量优先级更高，不会被 .env 覆盖。
    """
    env_path = Path(__file__).resolve().parent.parent / ".env"
    if not env_path.exists():
        return
    try:
        for raw_line in env_path.read_text(encoding="utf-8").splitlines():
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            key = key.strip()
            value = value.strip().strip('"').strip("'")
            if key and key not in os.environ:
                os.environ[key] = value
    except Exception:
        # .env 读取失败不应影响服务启动，静默忽略即可
        pass


_load_dotenv()


# ══════════════════════════════════════════════════════════════════
#  LLM 后端选择
#    api     —— 调用云端大模型 API（OpenAI 兼容，豆包/DeepSeek/通义/Kimi 等都能用）
#    ollama  —— 调用本机 Ollama
#    auto    —— 配置了 API_KEY 就用 api，否则退回 ollama
#  在 ai-service/.env 里改 LLM_BACKEND 即可切换
# ══════════════════════════════════════════════════════════════════
LLM_BACKEND = os.getenv("LLM_BACKEND", "auto").strip().lower()

# ── 云端 API 配置 ─────────────────────────────────────────────────
# 通用：任何 OpenAI 兼容接口都能用（火山方舟/DeepSeek/阿里百炼/Moonshot/智谱）
API_KEY = os.getenv("API_KEY", "").strip()
API_BASE_URL = os.getenv("API_BASE_URL", "https://api.deepseek.com/v1").strip().rstrip("/")
API_MODEL = os.getenv("API_MODEL", "deepseek-chat").strip()
# 模型能力档位：用于决定 temperature / 是否走 JSON 模式
API_TEMPERATURE = float(os.getenv("API_TEMPERATURE", "0.6"))
API_MAX_TOKENS = int(os.getenv("API_MAX_TOKENS", "2048"))

# OpenAI 兼容接口的等待上限（秒）。云端 API 通常几秒到几十秒
API_TIMEOUT = int(os.getenv("API_TIMEOUT", "120"))

# ── Ollama 配置 ───────────────────────────────────────────────────
OLLAMA_HOST = os.getenv("OLLAMA_HOST", "http://localhost:11434")

# ── 服务监听地址 ──────────────────────────────────────────────────
# 默认只听本机 127.0.0.1：本服务没有鉴权，若监听 0.0.0.0，
# 同一局域网/WiFi 下的其他人可以直接调用你的 AI 接口，消耗你的 API 额度。
# 确实需要从别的机器访问时，才改成 0.0.0.0（并自行做好网络隔离）。
AI_SERVICE_HOST = os.getenv("AI_SERVICE_HOST", "127.0.0.1").strip()
AI_SERVICE_PORT = int(os.getenv("AI_SERVICE_PORT", "8000"))

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


def resolved_backend() -> str:
    """把 auto 解析成实际后端名，返回 'api' 或 'ollama'。"""
    if LLM_BACKEND == "api":
        return "api"
    if LLM_BACKEND == "ollama":
        return "ollama"
    # auto：有 key 就走云端
    return "api" if API_KEY else "ollama"
