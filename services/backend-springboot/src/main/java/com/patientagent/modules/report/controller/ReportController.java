package com.patientagent.modules.report.controller;

import com.patientagent.common.response.ApiResponse;
import com.patientagent.modules.report.dto.MedicalRecordRefResponse;
import com.patientagent.modules.report.dto.ReportDetailResponse;
import com.patientagent.modules.report.dto.ReportInterpretResponse;
import com.patientagent.modules.report.dto.ReportListResponse;
import com.patientagent.modules.report.dto.ReportUpdateRequest;
import com.patientagent.modules.report.dto.ReportUploadResponse;
import com.patientagent.modules.report.service.ReportService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 医疗报告管理模块 HTTP 入口。
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 上传检查报告，支持 multipart 文件上传或直接提交 rawText。
     */
    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ApiResponse<ReportUploadResponse> uploadReport(
            @RequestParam Long userId,
            @RequestParam(required = false) Long medicalRecordId,
            @RequestParam String reportType,
            @RequestParam String reportTitle,
            @RequestParam(required = false) String hospitalName,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) String reportDate,
            @RequestParam(required = false) String rawText,
            @RequestParam(required = false) MultipartFile file
    ) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(
                traceId,
                reportService.uploadReport(
                        userId, medicalRecordId, reportType, reportTitle,
                        hospitalName, departmentName, reportDate, rawText, file
                )
        );
    }

    /**
     * 分页查询指定用户的报告列表，支持按类型/风险/时间范围/病历ID筛选。
     *
     * @param reportType      报告类型（可选，如 blood / ct / mri）
     * @param riskLevel       风险等级（可选，LOW/MEDIUM/HIGH/CRITICAL）
     * @param dateFrom        报告日期起始 yyyy-MM-dd（可选）
     * @param dateTo          报告日期截止 yyyy-MM-dd（可选）
     * @param medicalRecordId 关联病历ID（可选）
     */
    @GetMapping
    public ApiResponse<ReportListResponse> listReports(
            @RequestParam Long userId,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Long medicalRecordId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId,
                reportService.listReports(userId, reportType, riskLevel, dateFrom, dateTo, medicalRecordId, page, pageSize));
    }

    /**
     * 按报告编号查询详情。
     */
    @GetMapping("/{reportNo}")
    public ApiResponse<ReportDetailResponse> getReportDetail(
            @PathVariable String reportNo,
            @RequestParam Long userId
    ) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId, reportService.getReportDetail(userId, reportNo));
    }

    /**
     * 部分更新报告元信息（PATCH 语义，传入 null 字段不覆盖）。
     */
    @PatchMapping("/{reportNo}")
    public ApiResponse<ReportDetailResponse> updateReport(
            @PathVariable String reportNo,
            @RequestParam Long userId,
            @RequestBody ReportUpdateRequest request
    ) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId, reportService.updateReport(userId, reportNo, request));
    }

    /**
     * 软删除报告。
     */
    @DeleteMapping("/{reportNo}")
    public ApiResponse<Void> deleteReport(
            @PathVariable String reportNo,
            @RequestParam Long userId
    ) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        reportService.deleteReport(userId, reportNo);
        return ApiResponse.success(traceId, null);
    }

    /**
     * 执行报告 AI 解读。
     */
    @PostMapping("/{reportNo}/interpret")
    public ApiResponse<ReportInterpretResponse> interpretReport(
            @PathVariable String reportNo,
            @RequestParam Long userId
    ) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId, reportService.interpretReport(userId, reportNo));
    }

    /**
     * 查询报告关联的病历摘要（报告 → 病历反向查询）。
     */
    @GetMapping("/{reportNo}/medical-record")
    public ApiResponse<MedicalRecordRefResponse> getReportMedicalRecord(
            @PathVariable String reportNo,
            @RequestParam Long userId
    ) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId, reportService.getReportMedicalRecord(userId, reportNo));
    }
}
