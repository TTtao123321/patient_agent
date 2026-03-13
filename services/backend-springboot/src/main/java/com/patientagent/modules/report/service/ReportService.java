package com.patientagent.modules.report.service;

import com.patientagent.modules.report.dto.MedicalRecordRefResponse;
import com.patientagent.modules.report.dto.ReportDetailResponse;
import com.patientagent.modules.report.dto.ReportInterpretResponse;
import com.patientagent.modules.report.dto.ReportListResponse;
import com.patientagent.modules.report.dto.ReportUpdateRequest;
import com.patientagent.modules.report.dto.ReportUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 医疗报告管理服务接口。
 */
public interface ReportService {

    /**
     * 上传检查报告，支持文件与原始文本。
     */
    ReportUploadResponse uploadReport(
            Long userId,
            Long medicalRecordId,
            String reportType,
            String reportTitle,
            String hospitalName,
            String departmentName,
            String reportDate,
            String rawText,
            MultipartFile file
    );

    /**
     * 分页查询用户报告，支持按类型/风险/时间范围/病历ID筛选。
     *
     * @param reportType     报告类型过滤（可选）
     * @param riskLevel      风险等级过滤（LOW/MEDIUM/HIGH/CRITICAL，可选）
     * @param dateFrom       报告日期起始（yyyy-MM-dd，可选）
     * @param dateTo         报告日期截止（yyyy-MM-dd，可选）
     * @param medicalRecordId 关联病历ID过滤（可选）
     */
    ReportListResponse listReports(
            Long userId,
            String reportType,
            String riskLevel,
            String dateFrom,
            String dateTo,
            Long medicalRecordId,
            int page,
            int pageSize
    );

    /**
     * 查询单份报告详情。
     */
    ReportDetailResponse getReportDetail(Long userId, String reportNo);

    /**
     * 部分更新报告元信息（PATCH 语义，null 字段不覆盖）。
     */
    ReportDetailResponse updateReport(Long userId, String reportNo, ReportUpdateRequest request);

    /**
     * 软删除报告。
     */
    void deleteReport(Long userId, String reportNo);

    /**
     * 对指定报告执行 AI 解读并回写结果。
     */
    ReportInterpretResponse interpretReport(Long userId, String reportNo);

    /**
     * 查询报告关联的病历摘要（报告 → 病历方向）。
     */
    MedicalRecordRefResponse getReportMedicalRecord(Long userId, String reportNo);
}
