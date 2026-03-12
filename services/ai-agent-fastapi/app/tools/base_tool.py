"""Base tool class definition."""
from abc import ABC, abstractmethod
from typing import Any, Optional
from pydantic import BaseModel, Field


class ToolParameter(BaseModel):
    """Tool parameter definition."""
    name: str = Field(..., description="参数名")
    type: str = Field(..., description="参数类型: string, integer, float, boolean, array")
    description: str = Field(..., description="参数描述")
    required: bool = Field(default=True, description="是否必需")


class ToolDefinition(BaseModel):
    """Tool definition model."""
    name: str = Field(..., description="工具名称")
    description: str = Field(..., description="工具描述")
    parameters: list[ToolParameter] = Field(default_factory=list, description="参数列表")


class BaseTool(ABC):
    """Abstract base class for all tools."""
    
    def __init__(self) -> None:
        pass
    
    @property
    @abstractmethod
    def name(self) -> str:
        """Tool name."""
        raise NotImplementedError
    
    @property
    @abstractmethod
    def description(self) -> str:
        """Tool description."""
        raise NotImplementedError
    
    @property
    def parameters(self) -> list[ToolParameter]:
        """Tool parameters definition."""
        return []
    
    def get_definition(self) -> ToolDefinition:
        """Get tool definition."""
        return ToolDefinition(
            name=self.name,
            description=self.description,
            parameters=self.parameters,
        )
    
    @abstractmethod
    def execute(self, **kwargs) -> dict[str, Any]:
        """
        Execute the tool with given parameters.
        
        Returns:
            dict with 'success' and 'data' or 'error' keys
        """
        raise NotImplementedError
