"""LLM 客户端封装。

统一提供 invoke/stream 两种调用方式，并以单例形式复用底层模型连接。
"""

from collections.abc import Iterator

from langchain_ollama import OllamaLLM
from app.core.settings import settings


class LLMClient:
    """LLM 调用单例客户端。"""
    
    _instance = None
    _llm = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def __init__(self):
        """初始化底层 Ollama 模型实例。"""
        if self._llm is None:
            self._llm = OllamaLLM(
                base_url=settings.ollama_base_url,
                model=settings.ollama_model,
                # 低温度让输出更稳定，适合意图识别等结构化任务。
                temperature=0.3,
            )
    
    def get_llm(self) -> OllamaLLM:
        """返回底层 LLM 对象。"""
        return self._llm
    
    def invoke(self, prompt: str) -> str:
        """同步调用 LLM 并返回完整文本。"""
        return self._llm.invoke(prompt)

    def stream(self, prompt: str) -> Iterator[str]:
        """流式调用 LLM，产出增量文本块。"""
        for chunk in self._llm.stream(prompt):
            if chunk is None:
                continue
            if isinstance(chunk, str):
                if chunk:
                    yield chunk
                continue

            content = getattr(chunk, "content", None)
            if isinstance(content, str) and content:
                yield content
                continue

            text = str(chunk)
            if text:
                yield text


def get_llm_client() -> LLMClient:
    """获取全局 LLMClient 单例。"""
    return LLMClient()
