from abc import ABC, abstractmethod
import logging
import time
from typing import Any
from app.core.settings import settings
from app.observability.metrics.registry import metrics_registry
from app.tools.executor.tool_executor import ToolExecutor, ToolCall


logger = logging.getLogger(__name__)


class BaseAgent(ABC):
    """所有 Agent 的抽象基类。

    该类统一封装工具调用能力（单工具、多工具、工具列表），
    具体业务 Agent 只需关注 `handle` 的业务逻辑实现。
    """
    
    def __init__(self) -> None:
        self.tool_executor = ToolExecutor()
    
    @abstractmethod
    def handle(self, query: str) -> str:
        """处理用户问题并返回文本结果。"""
        raise NotImplementedError
    
    def call_tool(self, tool_name: str, **parameters) -> dict[str, Any]:
        """调用单个工具并返回统一结果结构。

        同时记录工具调用指标与日志，并在超时阈值时输出慢调用告警。
        """
        started_at = time.perf_counter()
        result = self.tool_executor.execute_tool(tool_name, **parameters)
        latency_ms = (time.perf_counter() - started_at) * 1000
        metrics_registry.record_tool_call(tool_name=tool_name, success=result.success)
        logger.info(
            "agent_tool_call tool=%s success=%s latency_ms=%.2f param_keys=%s",
            tool_name,
            result.success,
            latency_ms,
            sorted(list(parameters.keys())),
        )
        if latency_ms >= settings.slow_tool_call_threshold_ms:
            logger.warning(
                "slow_tool_call_detected tool=%s latency_ms=%.2f threshold_ms=%s",
                tool_name,
                latency_ms,
                settings.slow_tool_call_threshold_ms,
            )
        return {
            "success": result.success,
            "data": result.data,
            "error": result.error,
        }
    
    def call_tools(self, calls: list[ToolCall]) -> dict[str, Any]:
        """批量调用多个工具并汇总结果。"""
        response = self.tool_executor.execute_tools(calls)
        return {
            "results": [r.model_dump() for r in response.results],
            "total_calls": response.total_calls,
            "successful_calls": response.successful_calls,
        }
    
    def get_available_tools(self) -> list[dict[str, Any]]:
        """获取当前已注册的工具清单。"""
        return self.tool_executor.get_available_tools()
