import re
from typing import Any

from app.schemas.report.structured_report import (
    StructuredReportDto,
    IndicatorDto,
    FindingDto,
    ReportMetaDto,
)


class ReportStructuredParser:
    """报告结构化解析器。

    目标：从用户问题和原始报告文本中抽取指标、影像发现，
    并输出统一 StructuredReportDto，便于前后端稳定对接。
    """

    INDICATOR_RULES = {
        "白细胞": {
            "aliases": ["白细胞", "WBC", "白血球"],
            "unit": "x10^9/L",
            "reference_range": {"low": 4.0, "high": 10.0},
            "high_explanation": "白细胞升高常见于感染、炎症、应激反应等情况。",
            "low_explanation": "白细胞偏低可能与病毒感染、骨髓抑制或部分药物影响有关。",
            "normal_explanation": "白细胞处于参考范围内，通常提示当前无明显白细胞异常。",
        },
        "中性粒细胞百分比": {
            "aliases": ["中性粒细胞百分比", "中性粒细胞%", "NEUT%", "N%"],
            "unit": "%",
            "reference_range": {"low": 40.0, "high": 75.0},
            "high_explanation": "中性粒细胞比例升高常见于细菌感染、炎症或机体应激状态。",
            "low_explanation": "中性粒细胞比例偏低可见于病毒感染或部分血液系统问题。",
            "normal_explanation": "中性粒细胞比例正常，提示该项未见明显异常。",
        },
        "血红蛋白": {
            "aliases": ["血红蛋白", "Hb", "HGB"],
            "unit": "g/L",
            "reference_range": {"low": 120.0, "high": 160.0},
            "high_explanation": "血红蛋白升高可见于血液浓缩、吸烟或慢性缺氧等情况。",
            "low_explanation": "血红蛋白偏低提示贫血倾向，常见于缺铁、慢性失血或慢性疾病。",
            "normal_explanation": "血红蛋白处于参考范围内，氧运输能力相关指标基本正常。",
        },
        "血小板": {
            "aliases": ["血小板", "PLT"],
            "unit": "x10^9/L",
            "reference_range": {"low": 100.0, "high": 300.0},
            "high_explanation": "血小板升高可能与炎症、缺铁或骨髓增殖性疾病相关。",
            "low_explanation": "血小板偏低会增加出血风险，需结合症状和复查进一步评估。",
            "normal_explanation": "血小板处于参考范围内，凝血相关风险未见明显异常提示。",
        },
        "总胆固醇": {
            "aliases": ["总胆固醇", "TC"],
            "unit": "mmol/L",
            "reference_range": {"low": 0.0, "high": 5.2},
            "high_explanation": "总胆固醇升高会增加动脉粥样硬化和心血管疾病风险。",
            "low_explanation": "总胆固醇偏低通常临床意义较小，需结合营养状态和基础疾病判断。",
            "normal_explanation": "总胆固醇处于参考范围内。",
        },
        "低密度脂蛋白": {
            "aliases": ["低密度脂蛋白", "LDL", "LDL胆固醇"],
            "unit": "mmol/L",
            "reference_range": {"low": 0.0, "high": 3.4},
            "high_explanation": "低密度脂蛋白升高是冠心病和脑卒中的重要风险因素，需要控制饮食和药物治疗。",
            "low_explanation": "低密度脂蛋白偏低通常不需要特殊处理，但需排除营养不良等情况。",
            "normal_explanation": "低密度脂蛋白处于理想范围，心血管风险相对较低。",
        },
        "高密度脂蛋白": {
            "aliases": ["高密度脂蛋白", "HDL", "HDL胆固醇"],
            "unit": "mmol/L",
            "reference_range": {"low": 1.0, "high": 10.0},
            "high_explanation": "高密度脂蛋白升高是有利的，提示有较好的保护性因素。",
            "low_explanation": "高密度脂蛋白偏低会增加心血管疾病风险，需要增加有氧运动和调整生活方式。",
            "normal_explanation": "高密度脂蛋白处于正常范围，提示心血管保护相对良好。",
        },
        "甘油三酯": {
            "aliases": ["甘油三酯", "TG"],
            "unit": "mmol/L",
            "reference_range": {"low": 0.0, "high": 1.7},
            "high_explanation": "甘油三酯升高常见于摄入过多碳水化合物、酒精或肥胖，也与代谢综合征相关。",
            "low_explanation": "甘油三酯偏低通常临床意义不大，但需排除营养不良。",
            "normal_explanation": "甘油三酯处于参考范围内，代谢状况相对良好。",
        },
        "空腹血糖": {
            "aliases": ["空腹血糖", "FPG", "GLU", "血糖"],
            "unit": "mmol/L",
            "reference_range": {"low": 3.9, "high": 6.1},
            "high_explanation": "空腹血糖升高提示糖代谢异常风险，需结合复查或糖化血红蛋白评估。",
            "low_explanation": "空腹血糖偏低可能与进食不足、降糖药物或内分泌问题相关。",
            "normal_explanation": "空腹血糖处于参考范围内。",
        },
    }

    GENERIC_PATTERN = re.compile(
        r"(?P<name>[A-Za-z\u4e00-\u9fa5%]{2,20})\s*[:：]?\s*"
        r"(?P<value>-?\d+(?:\.\d+)?)\s*"
        r"(?P<unit>[A-Za-z0-9/^%.\u00b5\u03bcxX*\u4e00-\u9fa5-]{0,20})"
        r"(?:[^\n。；;]{0,24}?(?:参考范围|正常范围|参考值|范围|ref|range)\s*[:：]?\s*"
        r"(?P<low>-?\d+(?:\.\d+)?)\s*[-~至]\s*(?P<high>-?\d+(?:\.\d+)?))?",
        re.IGNORECASE,
    )

    IMAGING_KEYWORDS = ["磨玻璃影", "结节", "占位", "积液", "肿块", "斑片影"]

    def parse(self, query: str, reports: list[dict[str, Any]] | None = None) -> StructuredReportDto:
        """解析入口：综合 query + 报告列表，输出结构化结果。"""
        reports = reports or []
        selected_reports = self._select_relevant_reports(query=query, reports=reports)
        source_text = self._build_source_text(query=query, reports=selected_reports)
        indicators = self._parse_indicators(source_text)
        findings = self._parse_imaging_findings(source_text)
        overall_status = self._summarize_status(indicators=indicators, findings=findings)
        report_type = self._infer_report_type(query=query, reports=selected_reports)

        recent_reports = [
            ReportMetaDto(
                report_no=report.get("report_no", ""),
                report_title=report.get("report_title", ""),
                report_type=report.get("report_type", "blood"),  # type: ignore
                report_date=report.get("report_date", ""),
                risk_level=report.get("risk_level", "LOW"),  # type: ignore
                interpretation_summary=report.get("interpretation_summary", ""),
            )
            for report in selected_reports
        ]

        return StructuredReportDto(
            report_type=report_type,  # type: ignore
            overall_status=overall_status,  # type: ignore
            indicators=indicators,
            findings=findings,
            medical_advice=self._build_medical_advice(indicators=indicators, findings=findings),
            recent_reports=recent_reports,
        )

    def _build_source_text(self, query: str, reports: list[dict[str, Any]]) -> str:
        """拼接用于解析的源文本（问题 + 报告正文 + 摘要）。"""
        parts = [query]
        for report in reports:
            raw_text = report.get("raw_text")
            if raw_text:
                parts.append(str(raw_text))
            interpretation_summary = report.get("interpretation_summary")
            if interpretation_summary:
                parts.append(str(interpretation_summary))
        return "\n".join(part for part in parts if part)

    def _select_relevant_reports(self, query: str, reports: list[dict[str, Any]]) -> list[dict[str, Any]]:
        """按推断报告类型优先筛选相关报告。"""
        inferred_type = self._infer_report_type(query=query, reports=[])
        if inferred_type == "laboratory":
            filtered_reports = [report for report in reports if report.get("report_type") == "blood"]
            return filtered_reports or reports
        if inferred_type == "imaging":
            filtered_reports = [report for report in reports if report.get("report_type") in {"ct", "mr", "mri", "ultrasound"}]
            return filtered_reports or reports
        return reports

    def _parse_indicators(self, text: str) -> list[IndicatorDto]:
        """从文本中抽取检验指标并做异常判定。"""
        indicators: list[IndicatorDto] = []
        seen_names: set[str] = set()

        for match in self.GENERIC_PATTERN.finditer(text):
            raw_name = match.group("name").strip()
            canonical_name, rule = self._match_indicator_rule(raw_name)
            if not canonical_name or canonical_name in seen_names:
                continue

            value = float(match.group("value"))
            reference_range = self._resolve_reference_range(rule=rule, match=match)
            status = self._judge_status(value=value, reference_range=reference_range)
            explanation = self._build_indicator_explanation(name=canonical_name, rule=rule, status=status)

            indicators.append(
                IndicatorDto(
                    name=canonical_name,
                    value=value,
                    unit=self._resolve_unit(rule=rule, match=match),
                    reference_range=reference_range,
                    status=status,  # type: ignore
                    abnormal=status != "normal",
                    medical_explanation=explanation,
                )
            )
            seen_names.add(canonical_name)

        return indicators

    def _parse_imaging_findings(self, text: str) -> list[FindingDto]:
        """从文本中抽取影像学阳性发现。"""
        findings: list[FindingDto] = []
        normalized_text = text.replace("；", "。")
        for sentence in re.split(r"[。\n]", normalized_text):
            content = sentence.strip()
            if not content:
                continue
            if self._is_negative_finding(content):
                continue
            if not any(keyword in content for keyword in self.IMAGING_KEYWORDS):
                continue
            findings.append(
                FindingDto(
                    name=self._extract_finding_name(content),
                    status="abnormal",  # type: ignore
                    summary=content,
                    medical_explanation="影像学异常描述通常需要结合既往影像、症状和专科医生判断随访周期。",
                )
            )
        return findings

    def _match_indicator_rule(self, raw_name: str) -> tuple[str | None, dict[str, Any] | None]:
        lower_name = raw_name.lower()
        for canonical_name, rule in self.INDICATOR_RULES.items():
            aliases = [canonical_name, *rule["aliases"]]
            if any(alias.lower() == lower_name for alias in aliases):
                return canonical_name, rule
        return None, None

    def _resolve_reference_range(self, rule: dict[str, Any] | None, match: re.Match[str]) -> dict[str, float] | None:
        low = match.group("low")
        high = match.group("high")
        if low is not None and high is not None:
            return {"low": float(low), "high": float(high)}
        if rule and rule.get("reference_range"):
            return dict(rule["reference_range"])
        return None

    def _resolve_unit(self, rule: dict[str, Any] | None, match: re.Match[str]) -> str | None:
        unit = match.group("unit").strip()
        if unit:
            return unit
        if rule:
            return rule.get("unit")
        return None

    def _judge_status(self, value: float, reference_range: dict[str, float] | None) -> str:
        if not reference_range:
            return "unknown"
        if value < reference_range["low"]:
            return "low"
        if value > reference_range["high"]:
            return "high"
        return "normal"

    def _build_indicator_explanation(self, name: str, rule: dict[str, Any] | None, status: str) -> str:
        if not rule:
            return f"{name}已提取，但缺少明确的指标规则支持，建议结合原始报告复核。"
        if status == "high":
            return rule["high_explanation"]
        if status == "low":
            return rule["low_explanation"]
        if status == "normal":
            return rule["normal_explanation"]
        return f"{name}已识别，但暂时无法判断是否超出参考范围。"

    def _infer_report_type(self, query: str, reports: list[dict[str, Any]]) -> str:
        """推断报告大类（laboratory/imaging/general）。"""
        text = " ".join([query, *[str(report.get("report_title", "")) for report in reports]]).lower()
        if "ct" in text or "肺" in text or "影像" in text:
            return "imaging"
        if any(keyword in text for keyword in ["血", "白细胞", "血红蛋白", "血小板", "胆固醇", "脂蛋白", "甘油三酯", "血糖"]):
            return "laboratory"
        return "general"

    def _summarize_status(self, indicators: list[IndicatorDto], findings: list[FindingDto]) -> str:
        """汇总整体状态。"""
        abnormal_count = sum(1 for indicator in indicators if indicator.status in {"high", "low"})
        if abnormal_count >= 2 or findings:
            return "abnormal"
        if abnormal_count == 1:
            return "attention_needed"
        if indicators:
            return "normal"
        return "insufficient_data"

    def _build_medical_advice(self, indicators: list[IndicatorDto], findings: list[FindingDto]) -> list[str]:
        """生成面向患者的建议文案（含安全免责声明）。"""
        advice: list[str] = []
        abnormal_indicators = [indicator for indicator in indicators if indicator.status in {"high", "low"}]

        if abnormal_indicators:
            indicator_names = "、".join(indicator.name for indicator in abnormal_indicators)
            advice.append(f"{indicator_names}存在异常，建议结合症状、既往病史和原始报告向医生进一步确认。")
        if findings:
            advice.append("影像学异常建议按报告意见复查，必要时尽快至相关专科门诊评估。")
        if not advice:
            advice.append("目前提取到的指标未见明确异常，但仍应以医院正式报告和临床医生判断为准。")

        advice.append("结构化解析仅用于辅助理解，不能替代医生面诊和正式诊断。")
        return advice

    def _extract_finding_name(self, content: str) -> str:
        for keyword in self.IMAGING_KEYWORDS:
            if keyword in content:
                return keyword
        return "影像学异常"

    def _is_negative_finding(self, content: str) -> bool:
        negative_phrases = ["未见", "未提示", "无明显", "阴性", "未发现"]
        return any(phrase in content for phrase in negative_phrases)
