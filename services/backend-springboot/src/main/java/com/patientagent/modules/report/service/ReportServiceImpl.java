package com.patientagent.modules.report.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patientagent.client.agent.AgentClient;
import com.patientagent.modules.medicalrecord.entity.MedicalRecordEntity;
import com.patientagent.modules.medicalrecord.repository.MedicalRecordRepository;
import com.patientagent.modules.report.dto.MedicalRecordRefResponse;
import com.patientagent.modules.report.dto.ReportDetailResponse;
import com.patientagent.modules.report.dto.ReportInterpretResponse;
import com.patientagent.modules.report.dto.ReportItemResponse;
import com.patientagent.modules.report.dto.ReportListResponse;
import com.patientagent.modules.report.dto.ReportUpdateRequest;
import com.patientagent.modules.report.dto.ReportUploadResponse;
import com.patientagent.modules.report.entity.MedicalReportEntity;
import com.patientagent.modules.report.repository.MedicalReportRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 医疗报告管理服务实现。
 *
 * <p>核心能力：
 * <ul>
 *   <li>报告上传：支持文本直传（rawText）和 multipart 文件两种方式</li>
 *   <li>动态筛选列表：可按类型/风险等级/时间范围/病历ID组合过滤</li>
 *   <li>部分更新（PATCH）：仅覆盖传入的非空字段</li>
 *   <li>软删除：仅将 is_deleted 置 1，不物理删除数据</li>
 *   <li>AI 解读：调用 AgentClient 执行报告解读并回写 interpretation_summary / risk_level</li>
 *   <li>病历反向关联：通过报告中的 medicalRecordId 查询病历摘要</li>
 * </ul>
 *
 * <p>所有写操作均通过 @Transactional 保证原子性；只读方法使用 readOnly=true 优化资源占用。
 */
@Service
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MedicalReportRepository medicalReportRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AgentClient agentClient;
    private final ObjectMapper objectMapper;

    @Value("${app.report.upload-dir:uploads/reports}")
    private String uploadDir;

    public ReportServiceImpl(
            MedicalReportRepository medicalReportRepository,
            MedicalRecordRepository medicalRecordRepository,
            AgentClient agentClient,
            ObjectMapper objectMapper
    ) {
        this.medicalReportRepository = medicalReportRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.agentClient = agentClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ReportUploadResponse uploadReport(
            Long userId,
            Long medicalRecordId,
            String reportType,
            String reportTitle,
            String hospitalName,
            String departmentName,
            String reportDate,
            String rawText,
            MultipartFile file
    ) {
        validateUploadParams(userId, reportType, reportTitle, rawText, file);

        String fileUrl = null;
        if (file != null && !file.isEmpty()) {
            fileUrl = storeFile(file);
        }

        String finalRawText = rawText;
        if ((finalRawText == null || finalRawText.isBlank()) && file != null && !file.isEmpty() && isTextLikeFile(file.getOriginalFilename())) {
            finalRawText = readTextSafely(file);
        }

        MedicalReportEntity entity = new MedicalReportEntity();
        entity.setReportNo(generateReportNo());
        entity.setUserId(userId);
        entity.setMedicalRecordId(medicalRecordId);
        entity.setReportType(reportType.trim().toLowerCase());
        entity.setReportTitle(reportTitle.trim());
        entity.setHospitalName(trimToNull(hospitalName));
        entity.setDepartmentName(trimToNull(departmentName));
        entity.setReportDate(parseReportDate(reportDate));
        entity.setSourceType(file != null && !file.isEmpty() ? "UPLOAD" : "MANUAL");
        entity.setFileUrl(fileUrl);
        entity.setRawText(trimToNull(finalRawText));
        entity.setReviewStatus("PENDING");

        entity = medicalReportRepository.save(entity);

        ReportUploadResponse response = new ReportUploadResponse();
        response.setReportId(entity.getId());
        response.setReportNo(entity.getReportNo());
        response.setReportType(entity.getReportType());
        response.setReportTitle(entity.getReportTitle());
        response.setReportDate(entity.getReportDate().format(TS_FMT));
        response.setReviewStatus(entity.getReviewStatus());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportListResponse listReports(
            Long userId,
            String reportType,
            String riskLevel,
            String dateFrom,
            String dateTo,
            Long medicalRecordId,
            int page,
            int pageSize
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);

        // 将筛选起止日期（含当天全天）转换为 LocalDateTime，null 表示不限制
        LocalDateTime from = dateFrom != null && !dateFrom.isBlank() ? parseReportDate(dateFrom) : null;
        // dateTo + 23:59:59 确保当天最后一条也被包含
        LocalDateTime to = dateTo != null && !dateTo.isBlank() ? parseReportDate(dateTo).plusDays(1).minusSeconds(1) : null;

        // 使用 JPA Specification 构建动态 WHERE 条件，避免多个单参数查询方法的组合爆炸问题
        Specification<MedicalReportEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 基础必选条件：用户隔离 + 未删除
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.equal(root.get("isDeleted"), 0));
            // 可选筛选条件：类型（存储时已 toLowerCase，查询时同样转小写保持一致）
            if (reportType != null && !reportType.isBlank()) {
                predicates.add(cb.equal(root.get("reportType"), reportType.trim().toLowerCase()));
            }
            // 可选筛选条件：风险等级（存储时已 toUpperCase，查询时同样转大写）
            if (riskLevel != null && !riskLevel.isBlank()) {
                predicates.add(cb.equal(root.get("riskLevel"), riskLevel.trim().toUpperCase()));
            }
            // 可选筛选条件：时间范围（闭区间）
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("reportDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("reportDate"), to));
            }
            // 可选筛选条件：病历ID（支持病历→报告方向的关联查询）
            if (medicalRecordId != null) {
                predicates.add(cb.equal(root.get("medicalRecordId"), medicalRecordId));
            }
            // ORDER BY report_date DESC（query 不为 null 时设置排序）
            if (query != null) {
                query.orderBy(cb.desc(root.get("reportDate")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<MedicalReportEntity> reportPage = medicalReportRepository.findAll(
                spec, PageRequest.of(safePage - 1, safePageSize));

        List<ReportItemResponse> items = reportPage.getContent().stream()
                .map(this::toItem)
                .collect(Collectors.toList());

        ReportListResponse response = new ReportListResponse();
        response.setUserId(userId);
        response.setPage(safePage);
        response.setPageSize(safePageSize);
        response.setTotal(reportPage.getTotalElements());
        response.setItems(items);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDetailResponse getReportDetail(Long userId, String reportNo) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (reportNo == null || reportNo.isBlank()) {
            throw new IllegalArgumentException("reportNo is required");
        }

        MedicalReportEntity entity = medicalReportRepository.findByReportNoAndIsDeleted(reportNo, 0)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        if (!userId.equals(entity.getUserId())) {
            throw new IllegalArgumentException("No permission to access this report");
        }

        return toDetail(entity);
    }

    @Override
    @Transactional
    public ReportInterpretResponse interpretReport(Long userId, String reportNo) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (reportNo == null || reportNo.isBlank()) {
            throw new IllegalArgumentException("reportNo is required");
        }

        MedicalReportEntity entity = medicalReportRepository.findByReportNoAndIsDeleted(reportNo, 0)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        if (!userId.equals(entity.getUserId())) {
            throw new IllegalArgumentException("No permission to interpret this report");
        }

        String sourceText = resolveInterpretationText(entity);
        if (sourceText == null || sourceText.isBlank()) {
            throw new IllegalArgumentException("Report raw text is empty, please upload text content first");
        }

        String sessionNo = "RPT" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 999);
        String prompt = buildInterpretPrompt(entity, sourceText);
        String answer = agentClient.chat(sessionNo, userId, prompt);
        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException("AI interpretation is empty");
        }

        String riskLevel = inferRiskLevel(answer);
        entity.setInterpretationSummary(answer.trim());
        entity.setRiskLevel(riskLevel);
        entity.setReviewStatus("REVIEWED");
        entity.setParsedJson(buildParsedJson(entity.getParsedJson(), answer, riskLevel));
        medicalReportRepository.save(entity);

        ReportInterpretResponse response = new ReportInterpretResponse();
        response.setReportNo(entity.getReportNo());
        response.setInterpretationSummary(entity.getInterpretationSummary());
        response.setRiskLevel(entity.getRiskLevel());
        response.setReviewStatus(entity.getReviewStatus());
        return response;
    }

    /**
     * 部分更新报告元信息（PATCH 语义）。
     *
     * <p>遵循 PATCH 语义：仅更新请求体中非 null 的字段，未传入的字段保持原值不变。
     * 例如只传 {@code reportTitle} 时，其余字段原样保留。
     * rawText 和 hospitalName/departmentName 传空字符串时会被置为 null（清空语义）。
     */
    @Override
    @Transactional
    public ReportDetailResponse updateReport(Long userId, String reportNo, ReportUpdateRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (reportNo == null || reportNo.isBlank()) {
            throw new IllegalArgumentException("reportNo is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }

        MedicalReportEntity entity = medicalReportRepository.findByReportNoAndIsDeleted(reportNo, 0)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        // 鉴权：仅允许报告归属用户修改
        if (!userId.equals(entity.getUserId())) {
            throw new IllegalArgumentException("No permission to update this report");
        }

        // 逐字段 PATCH：只有传入非 null 时才覆盖原值
        if (request.getReportTitle() != null && !request.getReportTitle().isBlank()) {
            entity.setReportTitle(request.getReportTitle().trim());
        }
        if (request.getReportType() != null && !request.getReportType().isBlank()) {
            // reportType 统一存小写，保持与上传时的格式一致
            entity.setReportType(request.getReportType().trim().toLowerCase());
        }
        if (request.getHospitalName() != null) {
            // 传空字符串 → trimToNull 返回 null → 清空该字段
            entity.setHospitalName(trimToNull(request.getHospitalName()));
        }
        if (request.getDepartmentName() != null) {
            entity.setDepartmentName(trimToNull(request.getDepartmentName()));
        }
        if (request.getReportDate() != null && !request.getReportDate().isBlank()) {
            entity.setReportDate(parseReportDate(request.getReportDate()));
        }
        if (request.getRawText() != null) {
            entity.setRawText(trimToNull(request.getRawText()));
        }

        entity = medicalReportRepository.save(entity);
        return toDetail(entity);
    }

    /**
     * 软删除报告。
     *
     * <p>仅将 {@code is_deleted} 标志位置为 1，不物理删除数据库记录。
     * 删除后该报告在所有查询接口中均不可见（过滤条件固定为 isDeleted=0）。
     */
    @Override
    @Transactional
    public void deleteReport(Long userId, String reportNo) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (reportNo == null || reportNo.isBlank()) {
            throw new IllegalArgumentException("reportNo is required");
        }

        MedicalReportEntity entity = medicalReportRepository.findByReportNoAndIsDeleted(reportNo, 0)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        // 鉴权：仅允许报告归属用户删除
        if (!userId.equals(entity.getUserId())) {
            throw new IllegalArgumentException("No permission to delete this report");
        }

        // 逻辑删除：置标志位，@PreUpdate 会自动刷新 updatedAt
        entity.setIsDeleted(1);
        medicalReportRepository.save(entity);
    }

    /**
     * 查询报告关联的病历摘要（报告 → 病历方向）。
     *
     * <p>前提条件：报告上传时指定了 {@code medicalRecordId}，否则抛出业务异常。
     * 通过 medicalRecordRepository 跨模块只读查询病历核心字段，返回摘要信息，
     * 不暴露病历的完整诊疗记录（现病史/既往史等详细文本字段）。
     */
    @Override
    @Transactional(readOnly = true)
    public MedicalRecordRefResponse getReportMedicalRecord(Long userId, String reportNo) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (reportNo == null || reportNo.isBlank()) {
            throw new IllegalArgumentException("reportNo is required");
        }

        MedicalReportEntity report = medicalReportRepository.findByReportNoAndIsDeleted(reportNo, 0)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        // 鉴权：确保只能访问自己的报告
        if (!userId.equals(report.getUserId())) {
            throw new IllegalArgumentException("No permission to access this report");
        }

        // 检查报告是否关联了病历；未关联时明确提示
        if (report.getMedicalRecordId() == null) {
            throw new IllegalArgumentException("This report is not linked to any medical record");
        }

        // 跨模块只读查询病历实体（共享同一个数据库事务）
        MedicalRecordEntity record = medicalRecordRepository
                .findByIdAndIsDeleted(report.getMedicalRecordId(), 0)
                .orElseThrow(() -> new IllegalArgumentException("Associated medical record not found"));

        // 映射为轻量摘要 DTO，仅返回关键展示字段
        MedicalRecordRefResponse response = new MedicalRecordRefResponse();
        response.setRecordId(record.getId());
        response.setRecordNo(record.getRecordNo());
        response.setUserId(record.getUserId());
        response.setPatientName(record.getPatientName());
        response.setAge(record.getAge());
        response.setChiefComplaint(record.getChiefComplaint());
        response.setDiagnosisSummary(record.getDiagnosisSummary());
        response.setAttendingDoctor(record.getAttendingDoctor());
        response.setRecordDate(record.getRecordDate() == null ? null : record.getRecordDate().format(TS_FMT));
        return response;
    }

    private void validateUploadParams(
            Long userId,
            String reportType,
            String reportTitle,
            String rawText,
            MultipartFile file
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (reportType == null || reportType.isBlank()) {
            throw new IllegalArgumentException("reportType is required");
        }
        if (reportTitle == null || reportTitle.isBlank()) {
            throw new IllegalArgumentException("reportTitle is required");
        }

        boolean noText = rawText == null || rawText.isBlank();
        boolean noFile = file == null || file.isEmpty();
        if (noText && noFile) {
            throw new IllegalArgumentException("Either rawText or file must be provided");
        }
    }

    private String resolveInterpretationText(MedicalReportEntity entity) {
        if (entity.getRawText() != null && !entity.getRawText().isBlank()) {
            return entity.getRawText();
        }

        String fileUrl = entity.getFileUrl();
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }

        Path path = Paths.get(fileUrl);
        if (!Files.exists(path) || Files.isDirectory(path) || !isTextLikeFile(path.getFileName().toString())) {
            return null;
        }

        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return null;
        }
    }

    private String buildInterpretPrompt(MedicalReportEntity entity, String sourceText) {
        return "你是一名严谨的医学报告解读助手。请基于以下检查报告内容输出："
                + "1) 核心异常点；2) 风险分级（LOW/MEDIUM/HIGH/CRITICAL）；"
                + "3) 建议复查项目；4) 建议就诊科室。"
                + "请避免确诊语气，并明确提示‘仅供参考，以医生面诊为准’。\n\n"
                + "报告类型: " + entity.getReportType() + "\n"
                + "报告标题: " + entity.getReportTitle() + "\n"
                + "报告内容:\n" + sourceText;
    }

    private String inferRiskLevel(String answer) {
        String lower = answer.toLowerCase();
        if (lower.contains("critical") || lower.contains("危急") || lower.contains("紧急") || lower.contains("立即就医")) {
            return "CRITICAL";
        }
        if (lower.contains("high") || lower.contains("高风险") || lower.contains("明显异常") || lower.contains("高度怀疑")) {
            return "HIGH";
        }
        if (lower.contains("medium") || lower.contains("中风险") || lower.contains("需复查") || lower.contains("建议复诊")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String buildParsedJson(String existingJson, String answer, String riskLevel) {
        try {
            Map<String, Object> payload;
            if (existingJson == null || existingJson.isBlank()) {
                payload = new HashMap<>();
            } else {
                payload = objectMapper.readValue(existingJson, new TypeReference<Map<String, Object>>() {});
            }
            payload.put("ai_interpretation", answer);
            payload.put("risk_level", riskLevel);
            payload.put("interpreted_at", LocalDateTime.now().format(TS_FMT));
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build parsed_json", ex);
        }
    }

    private String storeFile(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String original = file.getOriginalFilename();
            String safeName = (original == null || original.isBlank()) ? "report.bin" : Paths.get(original).getFileName().toString();
            String storedName = System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(1000, 9999) + "_" + safeName;
            Path target = uploadPath.resolve(storedName);

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store report file", ex);
        }
    }

    private String readTextSafely(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return null;
        }
    }

    private boolean isTextLikeFile(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv") || lower.endsWith(".json");
    }

    private LocalDateTime parseReportDate(String reportDate) {
        if (reportDate == null || reportDate.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(reportDate);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(reportDate).atStartOfDay();
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("reportDate format should be yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss");
            }
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateReportNo() {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "R" + ts + random;
    }

    private ReportItemResponse toItem(MedicalReportEntity entity) {
        ReportItemResponse item = new ReportItemResponse();
        item.setReportId(entity.getId());
        item.setReportNo(entity.getReportNo());
        item.setReportType(entity.getReportType());
        item.setReportTitle(entity.getReportTitle());
        item.setHospitalName(entity.getHospitalName());
        item.setDepartmentName(entity.getDepartmentName());
        item.setReportDate(entity.getReportDate() == null ? null : entity.getReportDate().format(TS_FMT));
        item.setRiskLevel(entity.getRiskLevel());
        item.setReviewStatus(entity.getReviewStatus());
        return item;
    }

    private ReportDetailResponse toDetail(MedicalReportEntity entity) {
        ReportDetailResponse detail = new ReportDetailResponse();
        detail.setReportId(entity.getId());
        detail.setReportNo(entity.getReportNo());
        detail.setUserId(entity.getUserId());
        detail.setMedicalRecordId(entity.getMedicalRecordId());
        detail.setReportType(entity.getReportType());
        detail.setReportTitle(entity.getReportTitle());
        detail.setHospitalName(entity.getHospitalName());
        detail.setDepartmentName(entity.getDepartmentName());
        detail.setReportDate(entity.getReportDate() == null ? null : entity.getReportDate().format(TS_FMT));
        detail.setSourceType(entity.getSourceType());
        detail.setFileUrl(entity.getFileUrl());
        detail.setRawText(entity.getRawText());
        detail.setParsedJson(entity.getParsedJson());
        detail.setInterpretationSummary(entity.getInterpretationSummary());
        detail.setRiskLevel(entity.getRiskLevel());
        detail.setReviewStatus(entity.getReviewStatus());
        detail.setCreatedAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().format(TS_FMT));
        detail.setUpdatedAt(entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().format(TS_FMT));
        return detail;
    }
}
