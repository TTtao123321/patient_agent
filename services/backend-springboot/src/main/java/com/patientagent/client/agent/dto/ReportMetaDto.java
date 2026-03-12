package com.patientagent.client.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metadata of a single medical report.
 */
public class ReportMetaDto {

    @JsonProperty("report_no")
    private String reportNo;

    @JsonProperty("report_title")
    private String reportTitle;

    @JsonProperty("report_type")
    private String reportType; // "blood", "ct", "mri", "pathology", "ultrasound"

    @JsonProperty("report_date")
    private String reportDate; // format: YYYY-MM-DD

    @JsonProperty("risk_level")
    private String riskLevel; // "LOW", "MEDIUM", "HIGH"

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
