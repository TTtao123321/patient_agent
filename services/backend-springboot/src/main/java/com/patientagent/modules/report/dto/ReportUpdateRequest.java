package com.patientagent.modules.report.dto;

/**
 * 修改检查报告请求体（PATCH 语义，字段为 null 时不更新）。
 */
public class ReportUpdateRequest {

    private String reportTitle;
    private String reportType;
    private String hospitalName;
    private String departmentName;
    /** 格式: yyyy-MM-dd 或 yyyy-MM-ddTHH:mm:ss */
    private String reportDate;
    private String rawText;

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

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
}
