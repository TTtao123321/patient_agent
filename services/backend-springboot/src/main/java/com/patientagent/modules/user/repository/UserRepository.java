package com.patientagent.modules.user.repository;

import com.patientagent.modules.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByUsernameAndIsDeleted(String username, Integer isDeleted);

    boolean existsByPhoneAndIsDeleted(String phone, Integer isDeleted);

    Optional<UserEntity> findByUsernameAndIsDeleted(String username, Integer isDeleted);

    Optional<UserEntity> findByIdAndIsDeleted(Long id, Integer isDeleted);
}
