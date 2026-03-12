"""LLM Client for intent recognition and other LLM-based tasks."""

from langchain_ollama import OllamaLLM
from app.core.settings import settings


class LLMClient:
    """Singleton client for LLM operations."""
    
    _instance = None
    _llm = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def __init__(self):
        """Initialize LLM client."""
        if self._llm is None:
            self._llm = OllamaLLM(
                base_url=settings.ollama_base_url,
                model=settings.ollama_model,
                temperature=0.3,  # Lower temperature for more consistent intent classification
            )
    
    def get_llm(self) -> OllamaLLM:
        """Get LLM instance."""
        return self._llm
    
    def invoke(self, prompt: str) -> str:
        """
        Invoke LLM with given prompt.
        
        Args:
            prompt: Input prompt for LLM
        
        Returns:
            LLM response
        """
        return self._llm.invoke(prompt)


def get_llm_client() -> LLMClient:
    """Get or create LLM client instance."""
    return LLMClient()
