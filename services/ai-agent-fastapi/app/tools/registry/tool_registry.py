"""Tool registry for managing all available tools."""
from typing import Dict, Optional
from app.tools.base_tool import BaseTool, ToolDefinition
from app.tools.medical.medical_tools import (
    GetMedicalReportTool,
    GetMedicalRecordTool,
    SearchDrugTool,
    SearchDepartmentTool,
)


class ToolRegistry:
    """Registry for tool management and discovery."""
    
    def __init__(self) -> None:
        self._tools: Dict[str, BaseTool] = {}
        self._register_default_tools()
    
    def _register_default_tools(self) -> None:
        """Register all default medical tools."""
        medical_tools = [
            GetMedicalReportTool(),
            GetMedicalRecordTool(),
            SearchDrugTool(),
            SearchDepartmentTool(),
        ]
        
        for tool in medical_tools:
            self.register(tool)
    
    def register(self, tool: BaseTool) -> None:
        """
        Register a new tool.
        
        Args:
            tool: Tool instance to register
        """
        self._tools[tool.name] = tool
    
    def get_tool(self, name: str) -> Optional[BaseTool]:
        """
        Get tool by name.
        
        Args:
            name: Tool name
        
        Returns:
            Tool instance or None if not found
        """
        return self._tools.get(name)
    
    def list_tools(self) -> list[ToolDefinition]:
        """
        Get definitions of all available tools.
        
        Returns:
            List of tool definitions
        """
        return [tool.get_definition() for tool in self._tools.values()]
    
    def get_tools_dict(self) -> Dict[str, ToolDefinition]:
        """
        Get tools as dictionary for model context.
        
        Returns:
            Dictionary mapping tool name to definition
        """
        return {tool.name: tool.get_definition() for tool in self._tools.values()}
    
    def has_tool(self, name: str) -> bool:
        """
        Check if tool is registered.
        
        Args:
            name: Tool name
        
        Returns:
            True if tool exists, False otherwise
        """
        return name in self._tools


# Global registry instance
_registry: Optional[ToolRegistry] = None


def get_tool_registry() -> ToolRegistry:
    """Get or create the global tool registry."""
    global _registry
    if _registry is None:
        _registry = ToolRegistry()
    return _registry
