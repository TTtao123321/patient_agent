package com.patientagent.modules.medicalrecord.service;

import com.patientagent.modules.medicalrecord.entity.MedicalRecordEntity;
import com.patientagent.modules.medicalrecord.repository.MedicalRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 医疗记录服务实现。
 */
@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;

    public MedicalRecordServiceImpl(MedicalRecordRepository medicalRecordRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
    }

    @Override
    public Page<MedicalRecordEntity> getRecordsByUser(
            Long userId,
            int page,
            int pageSize
    ) {
        Sort sort = Sort.by(Sort.Direction.DESC, "recordDate");
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
        return medicalRecordRepository.findByUserIdAndIsDeletedFalse(userId, pageable);
    }

    @Override
    public List<MedicalRecordEntity> getAllRecordsByUser(Long userId) {
        return medicalRecordRepository.findByUserIdAndIsDeletedFalseOrderByRecordDateDesc(userId);
    }

    @Override
    public Optional<MedicalRecordEntity> getRecordById(Long id) {
        return medicalRecordRepository.findByIdAndIsDeletedFalse(id);
    }

    @Override
    public Optional<MedicalRecordEntity> getRecordByNo(String recordNo) {
        return medicalRecordRepository.findByRecordNoAndIsDeletedFalse(recordNo);
    }
}
