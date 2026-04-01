package com.benmake.transafe.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.benmake.transafe.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 用户数据访问层（Mapper 接口）
 *
 * <p>继承 MyBatis Plus 的 BaseMapper，自动获得以下基础 CRUD 方法：</p>
 * <ul>
 *   <li>selectById(id): 根据 ID 查询</li>
 *   <li>selectBatchIds(idList): 根据 ID 列表批量查询</li>
 *   <li>selectList(wrapper): 条件查询列表</li>
 *   <li>selectPage(page, wrapper): 分页查询</li>
 *   <li>insert(entity): 插入一条记录</li>
 *   <li>updateById(entity): 根据 ID 更新</li>
 *   <li>update(entity, wrapper): 条件更新</li>
 *   <li>deleteById(id): 根据 ID 删除</li>
 *   <li>delete(wrapper): 条件删除</li>
 * </ul>
 *
 * <h3>自定义方法说明</h3>
 * <p>本接口定义了基于用户名、邮箱、手机号的查询方法，用于：</p>
 * <ul>
 *   <li>用户登录：通过用户名查询</li>
 *   <li>注册校验：检查用户名/邮箱/手机号是否已存在</li>
 *   <li>密码找回：通过邮箱或手机号查询</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 使用继承的方法
 * UserEntity user = userMapper.selectById(1L);
 *
 * // 使用自定义方法
 * Optional&lt;UserEntity&gt; user = userMapper.findByUsername("admin");
 * boolean exists = userMapper.existsByEmail("test@example.com");
 * </pre>
 *
 * @author JTP
 * @since 2026-04-01
 * @see BaseMapper
 * @see UserEntity
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    /**
     * 根据用户名查询用户
     *
     * <p>用于登录验证，根据用户输入的用户名查询用户信息。</p>
     *
     * <h4>SQL 说明</h4>
     * <pre>
     * SELECT * FROM user WHERE username = ?
     * </pre>
     *
     * @param username 用户名，唯一标识
     * @return Optional&lt;UserEntity&gt; 用户实体，不存在则返回 empty
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    Optional<UserEntity> findByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     *
     * <p>用于：</p>
     * <ul>
     *   <li>邮箱登录</li>
     *   <li>找回密码</li>
     *   <li>注册时校验邮箱唯一性</li>
     * </ul>
     *
     * @param email 邮箱地址
     * @return Optional&lt;UserEntity&gt; 用户实体，不存在则返回 empty
     */
    @Select("SELECT * FROM user WHERE email = #{email}")
    Optional<UserEntity> findByEmail(@Param("email") String email);

    /**
     * 根据手机号查询用户
     *
     * <p>用于：</p>
     * <ul>
     *   <li>手机号登录</li>
     *   <li>找回密码</li>
     *   <li>注册时校验手机号唯一性</li>
     * </ul>
     *
     * @param phone 手机号
     * @return Optional&lt;UserEntity&gt; 用户实体，不存在则返回 empty
     */
    @Select("SELECT * FROM user WHERE phone = #{phone}")
    Optional<UserEntity> findByPhone(@Param("phone") String phone);

    /**
     * 检查用户名是否已存在
     *
     * <p>用于注册时校验用户名唯一性。</p>
     *
     * <h4>性能说明</h4>
     * <p>使用 COUNT(*) > 0 而非 SELECT * LIMIT 1，因为：</p>
     * <ul>
     *   <li>COUNT 只需扫描索引，不读取数据行</li>
     *   <li>返回 boolean 更直观</li>
     * </ul>
     *
     * @param username 用户名
     * @return true-已存在，false-不存在
     */
    @Select("SELECT COUNT(*) > 0 FROM user WHERE username = #{username}")
    boolean existsByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否已存在
     *
     * <p>用于注册时校验邮箱唯一性。</p>
     *
     * @param email 邮箱地址
     * @return true-已存在，false-不存在
     */
    @Select("SELECT COUNT(*) > 0 FROM user WHERE email = #{email}")
    boolean existsByEmail(@Param("email") String email);

    /**
     * 检查手机号是否已存在
     *
     * <p>用于注册时校验手机号唯一性。</p>
     *
     * @param phone 手机号
     * @return true-已存在，false-不存在
     */
    @Select("SELECT COUNT(*) > 0 FROM user WHERE phone = #{phone}")
    boolean existsByPhone(@Param("phone") String phone);
}