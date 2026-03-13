package com.patientagent.client.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 结构化医学报告解析结果全量 DTO。
 * <p>表示 Report Agent 对一批报告的解析输出，吻盖报告牡型、总体状态、
 * 检验指标列表、影像发现列表、医疗建议和近期报告列表。</p>
 */
public class StructuredReportDto {

    /** 报告类型：{@code laboratory}（检验）/ {@code imaging}（影像）/ {@code general}（通用）。 */
    @JsonProperty("report_type")
    private String reportType;

    /** 总体状态：{@code normal} / {@code attention_needed} / {@code abnormal} / {@code insufficient_data}。 */
    @JsonProperty("overall_status")
    private String overallStatus;

    /** 检验指标列表（适用于检验报告）。 */
    @JsonProperty("indicators")
    private List<IndicatorDto> indicators;

    /** 影像发现列表（适用于影像报告）。 */
    @JsonProperty("findings")
    private List<FindingDto> findings;

    /** Agent 生成的医疗建议条目列表。 */
    @JsonProperty("medical_advice")
    private List<String> medicalAdvice;

    /** 该用户最近的厉史报告元数据列表。 */
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
