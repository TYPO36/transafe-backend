package com.benmake.transafe.config;

import com.benmake.transafe.infra.mapper.UserMapper;
import com.benmake.transafe.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 超级管理员初始化器
 *
 * <p>应用启动时自动检查并创建超级管理员账户。</p>
 *
 * <h3>默认账户信息</h3>
 * <ul>
 *   <li>用户名: admin</li>
 *   <li>密码: admin123（生产环境请立即修改）</li>
 * </ul>
 *
 * @author JTP
 * @since 2026-04-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 默认管理员用户名
     */
    private static final String ADMIN_USERNAME = "admin";

    /**
     * 默认管理员密码（生产环境必须修改）
     */
    private static final String ADMIN_PASSWORD = "admin123";

    @Override
    public void run(String... args) {
        initAdminUser();
    }

    /**
     * 初始化超级管理员用户
     *
     * <p>如果管理员账户不存在，则自动创建。</p>
     */
    private void initAdminUser() {
        try {
            Optional<UserEntity> existingAdminOpt = userMapper.findByUsername(ADMIN_USERNAME);

            if (existingAdminOpt.isEmpty()) {
                UserEntity admin = new UserEntity();
                admin.setUsername(ADMIN_USERNAME);
                admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
                admin.setNickname("超级管理员");
                admin.setMembershipLevel(1);
                admin.setStatus("ACTIVE");
                admin.setRole("SUPER_ADMIN");

                userMapper.insert(admin);

                log.info("超级管理员账户已创建: username={}, password={}", ADMIN_USERNAME, ADMIN_PASSWORD);
                log.warn("【安全警告】请立即修改默认管理员密码！");
            } else {
                UserEntity existingAdmin = existingAdminOpt.get();
                // 确保 role 字段正确
                if (!"SUPER_ADMIN".equals(existingAdmin.getRole())) {
                    existingAdmin.setRole("SUPER_ADMIN");
                    userMapper.updateById(existingAdmin);
                    log.info("已更新管理员角色: username={}", ADMIN_USERNAME);
                }
                log.debug("管理员账户已存在: username={}", ADMIN_USERNAME);
            }
        } catch (Exception e) {
            log.error("初始化管理员账户失败", e);
        }
    }
}