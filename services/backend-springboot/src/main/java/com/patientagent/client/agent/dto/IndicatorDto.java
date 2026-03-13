package com.patientagent.client.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 检验报告中单个医学指标的数据传输对象。
 */
public class IndicatorDto {

    /** 指标名称，如「总胆固醇（TC）」。 */
    @JsonProperty("name")
    private String name;

    /** 指标数值。 */
    @JsonProperty("value")
    private Double value;

    /** 单位，如 mmol/L。 */
    @JsonProperty("unit")
    private String unit;

    /** 参考范围（下限和上限）。 */
    @JsonProperty("reference_range")
    private ReferencRangeDto referenceRange;

    /** 结果状态：{@code low}（偏低）/ {@code normal}（正常）/ {@code high}（偏高）/ {@code unknown}。 */
    @JsonProperty("status")
    private String status;

    /** 是否异常，如果超出参考范围则为 true。 */
    @JsonProperty("abnormal")
    private Boolean abnormal;

    /** 该指标的医学解释，用通俗语言向患者说明其全含义。 */
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
     * 参考范围简单 POJO，包含指标的正常下限和上限。
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
