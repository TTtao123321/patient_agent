package com.patientagent.modules.medicalrecord.dto;

import java.time.LocalDateTime;

/**
 * 医疗记录项目响应 DTO。
 */
public class RecordItemResponse {

    private String recordNo;
    private String patientName;
    private Integer age;
    private String chiefComplaint;
    private String diagnosisSummary;
    private String attendingDoctor;
    private LocalDateTime recordDate;

    public String getRecordNo() {
        return recordNo;
    }

    public void setRecordNo(String recordNo) {
        this.recordNo = recordNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public String getDiagnosisSummary() {
        return diagnosisSummary;
    }

    public void setDiagnosisSummary(String diagnosisSummary) {
        this.diagnosisSummary = diagnosisSummary;
    }

    public String getAttendingDoctor() {
        return attendingDoctor;
    }

    public void setAttendingDoctor(String attendingDoctor) {
        this.attendingDoctor = attendingDoctor;
    }

    public LocalDateTime getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDateTime recordDate) {
        this.recordDate = recordDate;
    }

    public static RecordItemResponse fromEntity(
            com.patientagent.modules.medicalrecord.entity.MedicalRecordEntity entity
    ) {
        RecordItemResponse response = new RecordItemResponse();
        response.setRecordNo(entity.getRecordNo());
        response.setPatientName(entity.getPatientName());
        response.setAge(entity.getAge());
        response.setChiefComplaint(entity.getChiefComplaint());
        response.setDiagnosisSummary(entity.getDiagnosisSummary());
        response.setAttendingDoctor(entity.getAttendingDoctor());
        response.setRecordDate(entity.getRecordDate());
        return response;
    }
}
