package com.patientagent.client.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 影像报告或其它检查报告中单个发现 / 检查结果的数据传输对象。
 */
public class FindingDto {

    /** 发现项目名称，如「肾结来-oversize」。 */
    @JsonProperty("name")
    private String name;

    /** 状态：{@code abnormal}（异常）或 {@code normal}（正常）。 */
    @JsonProperty("status")
    private String status;

    /** 对该发现的简短描述文字。 */
    @JsonProperty("summary")
    private String summary;

    /** 该发现的医学解释，用通俗语言向患者说明其全含义。 */
    @JsonProperty("medical_explanation")
    private String medicalExplanation;

    public FindingDto() {}

    public FindingDto(String name, String status, String summary, String medicalExplanation) {
        this.name = name;
        this.status = status;
        this.summary = summary;
        this.medicalExplanation = medicalExplanation;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getMedicalExplanation() {
        return medicalExplanation;
    }

    public void setMedicalExplanation(String medicalExplanation) {
        this.medicalExplanation = medicalExplanation;
    }
}
