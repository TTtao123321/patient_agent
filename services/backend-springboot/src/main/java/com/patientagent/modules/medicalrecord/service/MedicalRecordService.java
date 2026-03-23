package com.patientagent.modules.medicalrecord.service;

import com.patientagent.modules.medicalrecord.entity.MedicalRecordEntity;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

/**
 * 医疗记录服务接口。
 */
public interface MedicalRecordService {

    /**
     * 分页查询用户的医疗记录。
     *
     * @param userId   用户ID
     * @param page     页码（从1开始）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<MedicalRecordEntity> getRecordsByUser(
            Long userId,
            int page,
            int pageSize
    );

    /**
     * 获取用户的所有医疗记录（不分页，用于内部工具调用）。
     *
     * @param userId 用户ID
     * @return 记录列表
     */
    List<MedicalRecordEntity> getAllRecordsByUser(Long userId);

    /**
     * 根据ID获取记录。
     *
     * @param id 记录ID
     * @return 记录实体
     */
    Optional<MedicalRecordEntity> getRecordById(Long id);

    /**
     * 根据记录编号获取记录。
     *
     * @param recordNo 记录编号
     * @return 记录实体
     */
    Optional<MedicalRecordEntity> getRecordByNo(String recordNo);
}
