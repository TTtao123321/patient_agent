package com.patientagent.modules.medicalrecord.repository;

import com.patientagent.modules.medicalrecord.entity.MedicalRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 病历数据访问层（跨模块只读查询使用）。
 */
public interface MedicalRecordRepository extends JpaRepository<MedicalRecordEntity, Long> {

    /**
     * 按主键查询未删除的病历。
     */
    Optional<MedicalRecordEntity> findByIdAndIsDeleted(Long id, Integer isDeleted);
}
