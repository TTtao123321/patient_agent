"""Unit tests for report structured parser."""
import pytest

from app.services.report.report_structured_parser import ReportStructuredParser
from app.schemas.report.structured_report import StructuredReportDto


@pytest.fixture
def parser():
    return ReportStructuredParser()


class TestReportStructuredParser:
    """Tests for report structured parser."""

    def test_parse_blood_report_with_indicators(self, parser):
        """Test parsing blood report with lab indicators."""
        query = "白细胞12.5 x10^9/L，参考范围4.0-10.0；血红蛋白110 g/L，参考范围120-160"
        reports = []

        result = parser.parse(query=query, reports=reports)

        assert isinstance(result, StructuredReportDto)
        assert result.report_type == "laboratory"
        assert result.overall_status == "abnormal"
        assert len(result.indicators) >= 2
        
        # Check white blood cell (high)
        wbc = next(
            (ind for ind in result.indicators if ind.name == "白细胞"), None
        )
        assert wbc is not None
        assert wbc.value == 12.5
        assert wbc.status == "high"
        assert wbc.abnormal is True
        assert "感染" in wbc.medical_explanation
        
        # Check hemoglobin (low)
        hb = next(
            (ind for ind in result.indicators if ind.name == "血红蛋白"), None
        )
        assert hb is not None
        assert hb.value == 110.0
        assert hb.status == "low"
        assert hb.abnormal is True
        assert "贫血" in hb.medical_explanation

    def test_parse_lipid_report(self, parser):
        """Test parsing blood lipid report with multiple high values."""
        query = "总胆固醇6.5 mmol/L，参考范围0-5.2；低密度脂蛋白4.2 mmol/L，参考范围0-3.4"
        reports = []

        result = parser.parse(query=query, reports=reports)

        assert result.report_type == "laboratory"
        assert result.overall_status == "abnormal"
        assert len(result.indicators) >= 2
        
        # Check total cholesterol
        tc = next(
            (ind for ind in result.indicators if ind.name == "总胆固醇"), None
        )
        assert tc is not None
        assert tc.status == "high"
        assert "动脉粥样硬化" in tc.medical_explanation
        
        # Check LDL
        ldl = next(
            (ind for ind in result.indicators if ind.name == "低密度脂蛋白"), None
        )
        assert ldl is not None
        assert ldl.status == "high"

    def test_parse_glucose_report(self, parser):
        """Test parsing fasting blood glucose report."""
        query = "空腹血糖7.2 mmol/L，参考范围3.9-6.1"
        reports = []

        result = parser.parse(query=query, reports=reports)

        assert result.report_type == "laboratory"
        assert result.overall_status == "attention_needed"
        
        glucose = next(
            (ind for ind in result.indicators if "血糖" in ind.name), None
        )
        assert glucose is not None
        assert glucose.value == 7.2
        assert glucose.status == "high"
        assert "糖代谢异常" in glucose.medical_explanation

    def test_parse_imaging_report(self, parser):
        """Test parsing imaging report with findings."""
        query = "胸部CT"
        reports = [
            {
                "report_no": "RPT001",
                "report_type": "ct",
                "report_title": "胸部CT",
                "report_date": "2026-03-05",
                "risk_level": "MEDIUM",
                "interpretation_summary": "右上肺发现1.5cm磨玻璃影",
                "raw_text": "右上肺见约1.5cm磨玻璃影，边界欠清。",
                "hospital_name": "医院",
                "department_name": "放射科",
            }
        ]

        result = parser.parse(query=query, reports=reports)

        assert result.report_type == "imaging"
        assert result.overall_status == "abnormal"
        assert len(result.findings) > 0
        
        finding = result.findings[0]
        assert "磨玻璃影" in finding.name or "磨玻璃影" in finding.summary

    def test_parse_with_negative_findings(self, parser):
        """Test parsing report correctly excludes negative findings."""
        query = "CT"
        reports = [
            {
                "report_no": "RPT002",
                "report_type": "ct",
                "report_title": "胸部CT",
                "report_date": "2026-03-05",
                "risk_level": "LOW",
                "interpretation_summary": "未见明显异常",
                "raw_text": "双肺纹理清晰。未见明显胸腔积液。未见占位性病变。",
                "hospital_name": "医院",
                "department_name": "放射科",
            }
        ]

        result = parser.parse(query=query, reports=reports)

        assert result.report_type == "imaging"
        assert len(result.findings) == 0
        assert result.overall_status == "insufficient_data"

    def test_parse_normal_indicators(self, parser):
        """Test parsing report with all normal indicators."""
        query = "白细胞6.0 x10^9/L，参考范围4.0-10.0；血红蛋白140 g/L，参考范围120-160"
        reports = []

        result = parser.parse(query=query, reports=reports)

        assert result.overall_status == "normal"
        for indicator in result.indicators:
            assert indicator.abnormal is False
            assert indicator.status == "normal"

    def test_parse_includes_recent_reports_metadata(self, parser):
        """Test that parse result includes recent reports metadata."""
        query = "血常规"
        reports = [
            {
                "report_no": "RPT20260310001",
                "report_type": "blood",
                "report_title": "血液检查报告",
                "report_date": "2026-03-10",
                "risk_level": "MEDIUM",
                "interpretation_summary": "白细胞升高",
                "raw_text": "白细胞12.5",
                "hospital_name": "医院",
                "department_name": "检验科",
            }
        ]

        result = parser.parse(query=query, reports=reports)

        assert len(result.recent_reports) == 1
        assert result.recent_reports[0].report_no == "RPT20260310001"
        assert result.recent_reports[0].report_title == "血液检查报告"

    def test_parse_medical_advice_generated(self, parser):
        """Test that medical advice is generated for abnormal results."""
        query = "白细胞12.5 x10^9/L，参考范围4.0-10.0"
        reports = []

        result = parser.parse(query=query, reports=reports)

        assert len(result.medical_advice) >= 2
        assert any("医生" in advice for advice in result.medical_advice)
        assert any("辅助理解" in advice for advice in result.medical_advice)

    def test_report_filtering_blood_vs_imaging(self, parser):
        """Test that blood and imaging reports are filtered correctly."""
        # Query for blood report when multiple types available
        query = "血常规"
        reports = [
            {
                "report_no": "RPT001",
                "report_type": "blood",
                "report_title": "血液检查",
                "report_date": "2026-03-10",
                "risk_level": "LOW",
                "interpretation_summary": "正常",
                "raw_text": "白细胞6.0",
                "hospital_name": "医院",
                "department_name": "科室",
            },
            {
                "report_no": "RPT002",
                "report_type": "ct",
                "report_title": "CT报告",
                "report_date": "2026-03-05",
                "risk_level": "MEDIUM",
                "interpretation_summary": "有发现",
                "raw_text": "磨玻璃影",
                "hospital_name": "医院",
                "department_name": "科室",
            },
        ]

        result = parser.parse(query=query, reports=reports)

        assert result.report_type == "laboratory"
        assert len(result.recent_reports) == 1
        assert result.recent_reports[0].report_type == "blood"
