"""Tool calling API endpoints."""
from fastapi import APIRouter
from app.tools.executor.tool_executor import ToolExecutor
from app.schemas.http.tool_call import (
    AvailableToolsResponse,
    ExecuteToolRequest,
    ExecuteToolResponse,
    BatchExecuteToolsRequest,
    BatchExecuteToolsResponse,
    ToolDefinitionSchema,
    ToolExecutionResult,
)

router = APIRouter(prefix="/tools", tags=["tools"])
executor = ToolExecutor()


@router.get("/available", response_model=AvailableToolsResponse)
def list_available_tools() -> AvailableToolsResponse:
    """List all available tools."""
    tools_data = executor.get_available_tools()
    tools = [
        ToolDefinitionSchema(
            name=tool["name"],
            description=tool["description"],
            parameters=tool.get("parameters", []),
        )
        for tool in tools_data
    ]
    return AvailableToolsResponse(
        total_tools=len(tools),
        tools=tools,
    )


@router.post("/execute", response_model=ExecuteToolResponse)
def execute_tool(request: ExecuteToolRequest) -> ExecuteToolResponse:
    """Execute a single tool."""
    result = executor.execute_tool(request.tool_name, **request.parameters)
    return ExecuteToolResponse(
        result=ToolExecutionResult(
            tool_name=result.tool_name,
            success=result.success,
            data=result.data,
            error=result.error,
        )
    )


@router.post("/batch", response_model=BatchExecuteToolsResponse)
def batch_execute_tools(request: BatchExecuteToolsRequest) -> BatchExecuteToolsResponse:
    """Execute multiple tools in batch."""
    from app.tools.executor.tool_executor import ToolCall
    
    calls = [
        ToolCall(tool_name=call.tool_name, parameters=call.parameters)
        for call in request.tool_calls
    ]
    response = executor.execute_tools(calls)
    
    results = [
        ToolExecutionResult(
            tool_name=r.tool_name,
            success=r.success,
            data=r.data,
            error=r.error,
        )
        for r in response.results
    ]
    
    return BatchExecuteToolsResponse(
        total_calls=response.total_calls,
        successful_calls=response.successful_calls,
        results=results,
    )
