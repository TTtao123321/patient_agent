package com.patientagent.client.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Data transfer object for a single medical indicator extracted from a report.
 */
public class IndicatorDto {

    @JsonProperty("name")
    private String name;

    @JsonProperty("value")
    private Double value;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("reference_range")
    private ReferencRangeDto referenceRange;

    @JsonProperty("status")
    private String status; // "low", "normal", "high", "unknown"

    @JsonProperty("abnormal")
    private Boolean abnormal;

    @JsonProperty("medical_explanation")
    private String medicalExplanation;

    public IndicatorDto() {}

    public IndicatorDto(
            String name,
            Double value,
            String unit,
            ReferencRangeDto referenceRange,
            String status,
            Boolean abnormal,
            String medicalExplanation) {
        this.name = name;
        this.value = value;
        this.unit = unit;
        this.referenceRange = referenceRange;
        this.status = status;
        this.abnormal = abnormal;
        this.medicalExplanation = medicalExplanation;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public ReferencRangeDto getReferenceRange() {
        return referenceRange;
    }

    public void setReferenceRange(ReferencRangeDto referenceRange) {
        this.referenceRange = referenceRange;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getAbnormal() {
        return abnormal;
    }

    public void setAbnormal(Boolean abnormal) {
        this.abnormal = abnormal;
    }

    public String getMedicalExplanation() {
        return medicalExplanation;
    }

    public void setMedicalExplanation(String medicalExplanation) {
        this.medicalExplanation = medicalExplanation;
    }

    /**
     * Simple POJO for holding reference range (low, high).
     */
    public static class ReferencRangeDto {
        @JsonProperty("low")
        private Double low;

        @JsonProperty("high")
        private Double high;

        public ReferencRangeDto() {}

        public ReferencRangeDto(Double low, Double high) {
            this.low = low;
            this.high = high;
        }

        public Double getLow() {
            return low;
        }

        public void setLow(Double low) {
            this.low = low;
        }

        public Double getHigh() {
            return high;
        }

        public void setHigh(Double high) {
            this.high = high;
        }
    }
}
