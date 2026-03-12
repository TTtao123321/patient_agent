package com.patientagent.client.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data transfer object for a single finding/observation from imaging or other reports.
 */
public class FindingDto {

    @JsonProperty("name")
    private String name;

    @JsonProperty("status")
    private String status; // "abnormal", "normal"

    @JsonProperty("summary")
    private String summary;

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
