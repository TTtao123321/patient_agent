from abc import ABC, abstractmethod
from typing import Any, Optional
from app.tools.executor.tool_executor import ToolExecutor, ToolCall


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
        result = self.tool_executor.execute_tool(tool_name, **parameters)
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
