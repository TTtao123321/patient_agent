package com.patientagent.modules.user.repository;

import com.patientagent.modules.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 用户数据访问层。
 * <p>所有查询均过滤 {@code isDeleted = 0}，以支持软删除逻辑。</p>
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /** 检查用户名是否已被占用（注册时去重验证）。 */
    boolean existsByUsernameAndIsDeleted(String username, Integer isDeleted);

    /** 检查手机号是否已被占用（注册时去重验证）。 */
    boolean existsByPhoneAndIsDeleted(String phone, Integer isDeleted);

    /** 根据用户名查询用户，用于登录验证。 */
    Optional<UserEntity> findByUsernameAndIsDeleted(String username, Integer isDeleted);

    /** 根据主键 ID 查询用户，用于 Token 验证后获取用户信息。 */
    Optional<UserEntity> findByIdAndIsDeleted(Long id, Integer isDeleted);
}
