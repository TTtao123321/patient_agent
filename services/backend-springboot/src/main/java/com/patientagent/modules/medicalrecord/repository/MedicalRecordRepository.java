package com.patientagent.modules.medicalrecord.repository;

import com.patientagent.modules.medicalrecord.entity.MedicalRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 病历数据访问层（跨模块只读查询使用）。
 */
public interface MedicalRecordRepository extends JpaRepository<MedicalRecordEntity, Long> {

    /**
     * 按主键查询未删除的病历。
     */
    Optional<MedicalRecordEntity> findByIdAndIsDeleted(Long id, Integer isDeleted);

    /**
     * 按主键查询未删除的病历（isDeleted=0）。
     */
    Optional<MedicalRecordEntity> findByIdAndIsDeletedFalse(Long id);

    /**
     * 按记录编号查询未删除的病历。
     */
    Optional<MedicalRecordEntity> findByRecordNoAndIsDeletedFalse(String recordNo);

    /**
     * 分页查询用户的未删除病历。
     */
    Page<MedicalRecordEntity> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    /**
     * 查询用户的所有未删除病历，按记录日期倒序排列。
     */
    List<MedicalRecordEntity> findByUserIdAndIsDeletedFalseOrderByRecordDateDesc(Long userId);
}
