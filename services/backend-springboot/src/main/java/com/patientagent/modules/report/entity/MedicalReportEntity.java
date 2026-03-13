package com.patientagent.modules.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 医疗检查报告实体，对应 {@code medical_report} 表。
 */
@Entity
@Table(name = "medical_report")
public class MedicalReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_no", nullable = false, unique = true, length = 32)
    private String reportNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "medical_record_id")
    private Long medicalRecordId;

    @Column(name = "report_type", nullable = false, length = 32)
    private String reportType;

    @Column(name = "report_title", nullable = false, length = 128)
    private String reportTitle;

    @Column(name = "hospital_name", length = 128)
    private String hospitalName;

    @Column(name = "department_name", length = 64)
    private String departmentName;

    @Column(name = "report_date", nullable = false)
    private LocalDateTime reportDate;

    @Column(name = "source_type", nullable = false, length = 16)
    private String sourceType;

    @Column(name = "file_url", length = 255)
    private String fileUrl;

    @Column(name = "raw_text", columnDefinition = "longtext")
    private String rawText;

    @Column(name = "parsed_json", columnDefinition = "json")
    private String parsedJson;

    @Column(name = "interpretation_summary", columnDefinition = "text")
    private String interpretationSummary;

    @Column(name = "risk_level", nullable = false, length = 16)
    private String riskLevel;

    @Column(name = "review_status", nullable = false, length = 16)
    private String reviewStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.sourceType = this.sourceType == null ? "UPLOAD" : this.sourceType;
        this.riskLevel = this.riskLevel == null ? "LOW" : this.riskLevel;
        this.reviewStatus = this.reviewStatus == null ? "PENDING" : this.reviewStatus;
        this.isDeleted = this.isDeleted == null ? 0 : this.isDeleted;
        this.reportDate = this.reportDate == null ? now : this.reportDate;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReportNo() {
        return reportNo;
    }

    public void setReportNo(String reportNo) {
        this.reportNo = reportNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getMedicalRecordId() {
        return medicalRecordId;
    }

    public void setMedicalRecordId(Long medicalRecordId) {
        this.medicalRecordId = medicalRecordId;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
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

    public LocalDateTime getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDateTime reportDate) {
        this.reportDate = reportDate;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getParsedJson() {
        return parsedJson;
    }

    public void setParsedJson(String parsedJson) {
        this.parsedJson = parsedJson;
    }

    public String getInterpretationSummary() {
        return interpretationSummary;
    }

    public void setInterpretationSummary(String interpretationSummary) {
        this.interpretationSummary = interpretationSummary;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}
