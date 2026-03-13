package com.patientagent.modules.report.repository;

import com.patientagent.modules.report.entity.MedicalReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * 医疗报告数据访问层。
 */
public interface MedicalReportRepository extends JpaRepository<MedicalReportEntity, Long>,
        JpaSpecificationExecutor<MedicalReportEntity> {

    /**
     * 按报告编号查询未删除记录。
     */
    Optional<MedicalReportEntity> findByReportNoAndIsDeleted(String reportNo, Integer isDeleted);

    /**
     * 按主键和用户查询未删除报告，用于鉴权与详情读取。
     */
    Optional<MedicalReportEntity> findByIdAndUserIdAndIsDeleted(Long id, Long userId, Integer isDeleted);
}
