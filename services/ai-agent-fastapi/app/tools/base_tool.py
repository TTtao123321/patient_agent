"""工具抽象定义。"""
from abc import ABC, abstractmethod
from typing import Any, Optional
from pydantic import BaseModel, Field


class ToolParameter(BaseModel):
    """工具参数定义。"""
    name: str = Field(..., description="参数名")
    type: str = Field(..., description="参数类型: string, integer, float, boolean, array")
    description: str = Field(..., description="参数描述")
    required: bool = Field(default=True, description="是否必需")


class ToolDefinition(BaseModel):
    """工具元信息定义。"""
    name: str = Field(..., description="工具名称")
    description: str = Field(..., description="工具描述")
    parameters: list[ToolParameter] = Field(default_factory=list, description="参数列表")


class BaseTool(ABC):
    """所有工具的抽象基类。"""
    
    def __init__(self) -> None:
        pass
    
    @property
    @abstractmethod
    def name(self) -> str:
        """工具名称。"""
        raise NotImplementedError
    
    @property
    @abstractmethod
    def description(self) -> str:
        """工具说明。"""
        raise NotImplementedError
    
    @property
    def parameters(self) -> list[ToolParameter]:
        """工具参数定义。"""
        return []
    
    def get_definition(self) -> ToolDefinition:
        """获取工具定义。"""
        return ToolDefinition(
            name=self.name,
            description=self.description,
            parameters=self.parameters,
        )
    
    @abstractmethod
    def execute(self, **kwargs) -> dict[str, Any]:
        """执行工具并返回 `success/data/error` 结构。"""
        raise NotImplementedError
