from abc import ABC, abstractmethod
import logging
import time
from typing import Any
from app.core.settings import settings
from app.observability.metrics.registry import metrics_registry
from app.tools.executor.tool_executor import ToolExecutor, ToolCall


logger = logging.getLogger(__name__)


class BaseAgent(ABC):
    """Base class for all agents with tool calling capability."""
    
    def __init__(self) -> None:
        self.tool_executor = ToolExecutor()
    
    @abstractmethod
    def handle(self, query: str) -> str:
        """Handle query and return response."""
        raise NotImplementedError
    
    def call_tool(self, tool_name: str, **parameters) -> dict[str, Any]:
        """
        Call a tool and return result.
        
        Args:
            tool_name: Name of the tool to call
            **parameters: Tool parameters
        
        Returns:
            Tool execution result
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
        """
        Call multiple tools.
        
        Args:
            calls: List of tool calls
        
        Returns:
            All tool execution results
        """
        response = self.tool_executor.execute_tools(calls)
        return {
            "results": [r.model_dump() for r in response.results],
            "total_calls": response.total_calls,
            "successful_calls": response.successful_calls,
        }
    
    def get_available_tools(self) -> list[dict[str, Any]]:
        """Get list of available tools."""
        return self.tool_executor.get_available_tools()
