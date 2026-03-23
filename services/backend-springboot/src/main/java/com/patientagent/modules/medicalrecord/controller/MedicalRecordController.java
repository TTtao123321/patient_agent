package com.patientagent.modules.medicalrecord.controller;

import com.patientagent.common.response.ApiResponse;
import com.patientagent.modules.medicalrecord.dto.RecordItemResponse;
import com.patientagent.modules.medicalrecord.dto.RecordListResponse;
import com.patientagent.modules.medicalrecord.entity.MedicalRecordEntity;
import com.patientagent.modules.medicalrecord.service.MedicalRecordService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 医疗记录（病历）管理接口。
 */
@RestController
@RequestMapping("/api/v1/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    /**
     * 【内部工具用】根据用户ID查询记录（支持跨用户，内部调用）。
     * 注意：此接口暂时不对外暴露，仅供 Agent 工具通过认证调用。
     */
    @GetMapping("/internal/by-user")
    public ApiResponse<RecordListResponse> getRecordsByUserIdInternal(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        String traceId = UUID.randomUUID().toString().replace("-", "");

        Page<MedicalRecordEntity> recordPage = medicalRecordService.getRecordsByUser(
                userId, page, pageSize
        );

        RecordListResponse response = new RecordListResponse();
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotalPages(recordPage.getTotalPages());
        response.setTotalElements(recordPage.getTotalElements());
        response.setItems(recordPage.getContent().stream()
                .map(RecordItemResponse::fromEntity)
                .collect(Collectors.toList()));

        return ApiResponse.success(traceId, response);
    }
}
