"""工具注册中心。"""
from typing import Dict, Optional
from app.tools.base_tool import BaseTool, ToolDefinition
from app.tools.medical.medical_tools import (
    GetMedicalReportTool,
    GetMedicalRecordTool,
    SearchDrugTool,
    SearchDepartmentTool,
)


class ToolRegistry:
    """负责工具注册、查找与枚举。"""
    
    def __init__(self) -> None:
        self._tools: Dict[str, BaseTool] = {}
        self._register_default_tools()
    
    def _register_default_tools(self) -> None:
        """注册默认医疗工具。"""
        medical_tools = [
            GetMedicalReportTool(),
            GetMedicalRecordTool(),
            SearchDrugTool(),
            SearchDepartmentTool(),
        ]
        
        for tool in medical_tools:
            self.register(tool)
    
    def register(self, tool: BaseTool) -> None:
        """注册一个工具实例。"""
        self._tools[tool.name] = tool
    
    def get_tool(self, name: str) -> Optional[BaseTool]:
        """按名称获取工具。"""
        return self._tools.get(name)
    
    def list_tools(self) -> list[ToolDefinition]:
        """获取所有工具定义。"""
        return [tool.get_definition() for tool in self._tools.values()]
    
    def get_tools_dict(self) -> Dict[str, ToolDefinition]:
        """以字典形式返回工具定义。"""
        return {tool.name: tool.get_definition() for tool in self._tools.values()}
    
    def has_tool(self, name: str) -> bool:
        """检查工具是否已注册。"""
        return name in self._tools


# 全局注册中心单例。
_registry: Optional[ToolRegistry] = None


def get_tool_registry() -> ToolRegistry:
    """获取（或初始化）全局工具注册中心。"""
    global _registry
    if _registry is None:
        _registry = ToolRegistry()
    return _registry
