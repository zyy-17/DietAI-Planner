import os

OLLAMA_HOST = os.getenv("OLLAMA_HOST", "http://localhost:11434")

PREFERRED_MODELS = [
    "qwen2.5-coder:7b",
    "qwen2.5:7b",
    "qwen2.5:latest",
    "qwen2:7b",
    "qwen:latest",
]

MODEL_OPTIONS = {
    "temperature": 0.6,
    "num_ctx": 8192,
    "top_p": 0.9,
    "repeat_penalty": 1.1,
}

MAX_RETRIES = 5