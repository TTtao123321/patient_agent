from abc import ABC, abstractmethod


class BaseAgent(ABC):
    @abstractmethod
    def handle(self, query: str) -> str:
        raise NotImplementedError
