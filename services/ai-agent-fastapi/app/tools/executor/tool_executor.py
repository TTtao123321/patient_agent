"""Tool calling executor."""
from typing import Any, Optional
from pydantic import BaseModel, Field
from app.tools.registry.tool_registry import get_tool_registry


class ToolCall(BaseModel):
    """A single tool call request."""
    tool_name: str = Field(..., description="工具名称")
    parameters: dict[str, Any] = Field(default_factory=dict, description="工具参数")


class ToolCallResult(BaseModel):
    """Result of a tool call."""
    tool_name: str = Field(..., description="工具名称")
    success: bool = Field(..., description="是否执行成功")
    data: Optional[Any] = Field(None, description="执行结果数据")
    error: Optional[str] = Field(None, description="错误信息")


class ToolCallRequest(BaseModel):
    """Request containing multiple tool calls."""
    calls: list[ToolCall] = Field(..., description="工具调用列表")


class ToolCallResponse(BaseModel):
    """Response containing tool call results."""
    results: list[ToolCallResult] = Field(..., description="工具执行结果列表")
    total_calls: int = Field(..., description="总调用数")
    successful_calls: int = Field(..., description="成功调用数")


class ToolExecutor:
    """Executor for tool calling."""
    
    def __init__(self) -> None:
        self.registry = get_tool_registry()
    
    def execute_tool(self, tool_name: str, **parameters) -> ToolCallResult:
        """
        Execute a single tool.
        
        Args:
            tool_name: Name of the tool to execute
            **parameters: Tool parameters
        
        Returns:
            ToolCallResult with execution result
        """
        tool = self.registry.get_tool(tool_name)
        if not tool:
            return ToolCallResult(
                tool_name=tool_name,
                success=False,
                error=f"Tool '{tool_name}' not found",
            )
        
        try:
            result = tool.execute(**parameters)
            return ToolCallResult(
                tool_name=tool_name,
                success=result.get("success", False),
                data=result.get("data"),
                error=result.get("error"),
            )
        except Exception as e:
            return ToolCallResult(
                tool_name=tool_name,
                success=False,
                error=f"Tool execution failed: {str(e)}",
            )
    
    def execute_tools(self, calls: list[ToolCall]) -> ToolCallResponse:
        """
        Execute multiple tools.
        
        Args:
            calls: List of tool calls
        
        Returns:
            ToolCallResponse with all results
        """
        results = []
        successful = 0
        
        for call in calls:
            result = self.execute_tool(call.tool_name, **call.parameters)
            if result.success:
                successful += 1
            results.append(result)
        
        return ToolCallResponse(
            results=results,
            total_calls=len(calls),
            successful_calls=successful,
        )
    
    def get_available_tools(self) -> list[dict[str, Any]]:
        """Get list of available tools."""
        tools = self.registry.list_tools()
        return [
            {
                "name": tool.name,
                "description": tool.description,
                "parameters": [
                    {
                        "name": p.name,
                        "type": p.type,
                        "description": p.description,
                        "required": p.required,
                    }
                    for p in tool.parameters
                ],
            }
            for tool in tools
        ]
