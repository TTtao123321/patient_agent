package com.patientagent.client.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 单张医学报告的元数据，包含报告编号、类型、日期和风险等级等。
 */
public class ReportMetaDto {

    /** 报告唯一编号。 */
    @JsonProperty("report_no")
    private String reportNo;

    /** 报告标题，如「血常规检测报告」。 */
    @JsonProperty("report_title")
    private String reportTitle;

    /** 报告类型：{@code blood} / {@code ct} / {@code mri} / {@code pathology} / {@code ultrasound} 等。 */
    @JsonProperty("report_type")
    private String reportType;

    /** 报告日期，格式：{@code YYYY-MM-DD}。 */
    @JsonProperty("report_date")
    private String reportDate;

    /** 风险等级：{@code LOW} / {@code MEDIUM} / {@code HIGH}。 */
    @JsonProperty("risk_level")
    private String riskLevel;

    /** 报告解读总结， Agent 生成的一句话概述。 */
    @JsonProperty("interpretation_summary")
    private String interpretationSummary;

    public ReportMetaDto() {}

    public ReportMetaDto(
            String reportNo,
            String reportTitle,
            String reportType,
            String reportDate,
            String riskLevel,
            String interpretationSummary) {
        this.reportNo = reportNo;
        this.reportTitle = reportTitle;
        this.reportType = reportType;
        this.reportDate = reportDate;
        this.riskLevel = riskLevel;
        this.interpretationSummary = interpretationSummary;
    }

    // Getters and setters
    public String getReportNo() {
        return reportNo;
    }

    public void setReportNo(String reportNo) {
        this.reportNo = reportNo;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getInterpretationSummary() {
        return interpretationSummary;
    }

    public void setInterpretationSummary(String interpretationSummary) {
        this.interpretationSummary = interpretationSummary;
    }
}
