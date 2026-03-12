package com.patientagent.client.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Complete structured medical report analysis result.
 * This represents the output from the Report Agent's structured parsing.
 */
public class StructuredReportDto {

    @JsonProperty("report_type")
    private String reportType; // "laboratory", "imaging", "general"

    @JsonProperty("overall_status")
    private String overallStatus; // "normal", "attention_needed", "abnormal", "insufficient_data"

    @JsonProperty("indicators")
    private List<IndicatorDto> indicators;

    @JsonProperty("findings")
    private List<FindingDto> findings;

    @JsonProperty("medical_advice")
    private List<String> medicalAdvice;

    @JsonProperty("recent_reports")
    private List<ReportMetaDto> recentReports;

    public StructuredReportDto() {}

    public StructuredReportDto(
            String reportType,
            String overallStatus,
            List<IndicatorDto> indicators,
            List<FindingDto> findings,
            List<String> medicalAdvice,
            List<ReportMetaDto> recentReports) {
        this.reportType = reportType;
        this.overallStatus = overallStatus;
        this.indicators = indicators;
        this.findings = findings;
        this.medicalAdvice = medicalAdvice;
        this.recentReports = recentReports;
    }

    // Getters and setters
    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public List<IndicatorDto> getIndicators() {
        return indicators;
    }

    public void setIndicators(List<IndicatorDto> indicators) {
        this.indicators = indicators;
    }

    public List<FindingDto> getFindings() {
        return findings;
    }

    public void setFindings(List<FindingDto> findings) {
        this.findings = findings;
    }

    public List<String> getMedicalAdvice() {
        return medicalAdvice;
    }

    public void setMedicalAdvice(List<String> medicalAdvice) {
        this.medicalAdvice = medicalAdvice;
    }

    public List<ReportMetaDto> getRecentReports() {
        return recentReports;
    }

    public void setRecentReports(List<ReportMetaDto> recentReports) {
        this.recentReports = recentReports;
    }

    @Override
    public String toString() {
        return "StructuredReportDto{"
                + "reportType='"
                + reportType
                + '\''
                + ", overallStatus='"
                + overallStatus
                + '\''
                + ", indicators="
                + indicators
                + ", findings="
                + findings
                + ", medicalAdvice="
                + medicalAdvice
                + ", recentReports="
                + recentReports
                + '}';
    }
}
