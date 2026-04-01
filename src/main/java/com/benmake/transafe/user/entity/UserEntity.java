package com.benmake.transafe.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * <p>对应数据库表：user</p>
 *
 * <h3>表结构</h3>
 * <pre>
 * CREATE TABLE `user` (
 *   `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
 *   `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
 *   `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
 *   `username` VARCHAR(50) NOT NULL COMMENT '用户名',
 *   `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
 *   `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
 *   `avatar` VARCHAR(200) DEFAULT NULL COMMENT '头像URL',
 *   `membership_level` INT DEFAULT 0 COMMENT '会员等级：0-普通，1-VIP',
 *   `balance` DECIMAL(10,2) DEFAULT 0.00 COMMENT '账户余额',
 *   `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-正常，DISABLED-禁用',
 *   `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 *   `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 *   PRIMARY KEY (`id`),
 *   UNIQUE KEY `uk_username` (`username`),
 *   UNIQUE KEY `uk_email` (`email`),
 *   UNIQUE KEY `uk_phone` (`phone`)
 * );
 * </pre>
 *
 * <h3>MyBatis Plus 注解说明</h3>
 * <ul>
 *   <li>@TableName: 指定数据库表名</li>
 *   <li>@TableId: 指定主键字段，IdType.AUTO 表示自增</li>
 *   <li>@TableField: 指定字段映射，value 为数据库列名</li>
 * </ul>
 *
 * @author JTP
 * @since 2026-04-01
 */
@Data
@TableName("user")
public class UserEntity {

    /**
     * 主键 ID
     *
     * <p>数据库自增，插入时不需要设置。</p>
     *
     * <p>@TableId 说明：</p>
     * <ul>
     *   <li>type = IdType.AUTO: 数据库自增</li>
     *   <li>type = IdType.ASSIGN_ID: 雪花算法生成 Long 型 ID</li>
     *   <li>type = IdType.ASSIGN_UUID: 生成 UUID</li>
     * </ul>
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 邮箱地址
     *
     * <p>可选字段，用于邮箱登录和找回密码。</p>
     * <p>唯一约束，注册时需要校验。</p>
     */
    @TableField("email")
    private String email;

    /**
     * 手机号
     *
     * <p>可选字段，用于手机号登录和找回密码。</p>
     * <p>唯一约束，注册时需要校验。</p>
     */
    @TableField("phone")
    private String phone;

    /**
     * 用户名
     *
     * <p>必填字段，用户登录的唯一标识。</p>
     * <p>唯一约束，长度限制 50 字符。</p>
     */
    @TableField("username")
    private String username;

    /**
     * 密码
     *
     * <p>必填字段，使用 BCrypt 加密存储。</p>
     *
     * <h4>加密说明</h4>
     * <ul>
     *   <li>算法：BCrypt（Spring Security 默认）</li>
     *   <li>长度：加密后约 60 字符</li>
     *   <li>强度：默认 10 轮加密</li>
     * </ul>
     */
    @TableField("password")
    private String password;

    /**
     * 昵称
     *
     * <p>可选字段，用户显示名称。</p>
     * <p>默认值：注册时设置为用户名。</p>
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 头像 URL
     *
     * <p>可选字段，存储头像图片的访问地址。</p>
     * <p>可以是本地存储路径或 CDN 地址。</p>
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 会员等级
     *
     * <p>会员等级说明：</p>
     * <ul>
     *   <li>0: 普通用户</li>
     *   <li>1: VIP 用户（更高配额、优先处理）</li>
     * </ul>
     *
     * <p>默认值：0（普通用户）</p>
     */
    @TableField("membership_level")
    private Integer membershipLevel = 0;

    /**
     * 账户余额
     *
     * <p>用于付费功能，单位：元。</p>
     * <p>使用 BigDecimal 避免浮点数精度问题。</p>
     *
     * <p>默认值：0.00</p>
     */
    @TableField("balance")
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * 账户状态
     *
     * <p>状态说明：</p>
     * <ul>
     *   <li>ACTIVE: 正常，可正常使用</li>
     *   <li>DISABLED: 禁用，无法登录</li>
     *   <li>BANNED: 封禁，违规封禁</li>
     * </ul>
     *
     * <p>默认值：ACTIVE</p>
     */
    @TableField("status")
    private String status = "ACTIVE";

    /**
     * 用户角色
     *
     * <p>角色说明：</p>
     * <ul>
     *   <li>USER: 普通用户</li>
     *   <li>ADMIN: 管理员</li>
     *   <li>SUPER_ADMIN: 超级管理员</li>
     * </ul>
     *
     * <p>默认值：USER</p>
     */
    @TableField("role")
    private String role = "USER";

    /**
     * 创建时间
     *
     * <p>记录创建时间，注册时自动设置。</p>
     *
     * <h4>自动填充配置</h4>
     * <p>MyBatis Plus 自动填充，插入时自动设置。</p>
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * <p>记录最后更新时间，修改时自动更新。</p>
     *
     * <h4>自动填充配置</h4>
     * <p>MyBatis Plus 自动填充，插入和更新时自动设置。</p>
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记
     *
     * <p>0-未删除, 1-已删除</p>
     */
    @TableLogic
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    private Integer deleted = 0;
}