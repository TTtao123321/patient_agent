"""工具调用 API 的请求/响应模型定义。"""
from pydantic import BaseModel, Field
from typing import Any, Optional


class ToolDefinitionSchema(BaseModel):
    """Tool definition for API response."""
    name: str = Field(..., description="工具名称")
    description: str = Field(..., description="工具描述")
    parameters: list[dict[str, Any]] = Field(default_factory=list, description="工具参数列表")


class AvailableToolsResponse(BaseModel):
    """Response for listing available tools."""
    total_tools: int = Field(..., description="总工具数量")
    tools: list[ToolDefinitionSchema] = Field(..., description="工具列表")


class ExecuteToolRequest(BaseModel):
    """Request for executing a tool."""
    tool_name: str = Field(..., description="工具名称")
    parameters: dict[str, Any] = Field(default_factory=dict, description="工具参数")


class ToolExecutionResult(BaseModel):
    """Result of a single tool execution."""
    tool_name: str = Field(..., description="工具名称")
    success: bool = Field(..., description="是否成功")
    data: Optional[Any] = Field(None, description="执行结果数据")
    error: Optional[str] = Field(None, description="错误信息")


class ExecuteToolResponse(BaseModel):
    """Response for tool execution."""
    result: ToolExecutionResult = Field(..., description="执行结果")


class BatchExecuteToolsRequest(BaseModel):
    """Request for executing multiple tools."""
    tool_calls: list[ExecuteToolRequest] = Field(..., description="工具调用列表")


class BatchExecuteToolsResponse(BaseModel):
    """Response for batch tool execution."""
    total_calls: int = Field(..., description="总调用数")
    successful_calls: int = Field(..., description="成功调用数")
    results: list[ToolExecutionResult] = Field(..., description="执行结果列表")
