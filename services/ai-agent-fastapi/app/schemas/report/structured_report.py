"""报告结构化解析模型定义。"""
from typing import Literal

from pydantic import BaseModel, Field


class IndicatorDto(BaseModel):
    """报告中抽取的单个检验指标。"""

    name: str = Field(..., description="指标名称，如 白细胞、血红蛋白")
    value: float = Field(..., description="检测数值")
    unit: str | None = Field(None, description="测量单位，如 x10^9/L、g/L")
    reference_range: dict[str, float] | None = Field(
        None, description="参考范围，如 {'low': 4.0, 'high': 10.0}"
    )
    status: Literal["low", "normal", "high", "unknown"] = Field(
        ..., description="状态判断：偏低、正常、偏高或无法判断"
    )
    abnormal: bool = Field(..., description="是否异常")
    medical_explanation: str = Field(..., description="医学解释说明")


class FindingDto(BaseModel):
    """报告中抽取的单个影像/观察发现。"""

    name: str = Field(..., description="发现名称，如 磨玻璃影、结节")
    status: Literal["abnormal", "normal"] = Field(
        ..., description="发现状态"
    )
    summary: str = Field(..., description="发现的具体描述")
    medical_explanation: str = Field(
        ..., description="医学解释和临床建议"
    )


class ReportMetaDto(BaseModel):
    """单份医疗报告的元信息。"""

    report_no: str = Field(..., description="报告编号")
    report_title: str = Field(..., description="报告标题，如 血液检查报告")
    report_type: Literal["blood", "ct", "mri", "pathology", "ultrasound"] = Field(
        ..., description="报告类型"
    )
    report_date: str = Field(..., description="报告日期，格式 YYYY-MM-DD")
    risk_level: Literal["LOW", "MEDIUM", "HIGH"] = Field(
        ..., description="风险等级"
    )
    interpretation_summary: str = Field(
        ..., description="报告摘要或解读"
    )


class StructuredReportDto(BaseModel):
    """完整结构化报告解析结果。"""

    report_type: Literal["laboratory", "imaging", "general"] = Field(
        ..., description="报告大分类：化验类、影像类或其他"
    )
    overall_status: Literal[
        "normal", "attention_needed", "abnormal", "insufficient_data"
    ] = Field(..., description="整体健康状态评估")
    indicators: list[IndicatorDto] = Field(
        default_factory=list, description="提取的医学指标列表"
    )
    findings: list[FindingDto] = Field(
        default_factory=list, description="影像学或其他异常发现列表"
    )
    medical_advice: list[str] = Field(
        ..., description="医学建议列表，包含临床指导和安全免责声明"
    )
    recent_reports: list[ReportMetaDto] = Field(
        default_factory=list, description="关联的原始报告元数据"
    )

    model_config = {
        "json_schema_extra": {
            "example": {
                "report_type": "laboratory",
                "overall_status": "abnormal",
                "indicators": [
                    {
                        "name": "白细胞",
                        "value": 12.5,
                        "unit": "x10^9/L",
                        "reference_range": {"low": 4.0, "high": 10.0},
                        "status": "high",
                        "abnormal": True,
                        "medical_explanation": "白细胞升高常见于感染、炎症、应激反应等情况。",
                    }
                ],
                "findings": [],
                "medical_advice": [
                    "白细胞存在异常，建议结合症状、既往病史和原始报告向医生进一步确认。",
                    "结构化解析仅用于辅助理解，不能替代医生面诊和正式诊断。",
                ],
                "recent_reports": [
                    {
                        "report_no": "RPT20260310001",
                        "report_title": "血液检查报告",
                        "report_type": "blood",
                        "report_date": "2026-03-10",
                        "risk_level": "MEDIUM",
                        "interpretation_summary": "白细胞轻度升高，血红蛋白偏低，提示可能存在感染和轻度贫血倾向",
                    }
                ],
            }
        }
    }
