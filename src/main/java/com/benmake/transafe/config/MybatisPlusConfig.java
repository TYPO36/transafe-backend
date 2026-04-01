package com.benmake.transafe.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 配置类
 *
 * <p>本配置类用于配置 MyBatis Plus 的核心功能，包括：</p>
 * <ul>
 *   <li>分页插件：支持数据库分页查询，自动生成分页 SQL</li>
 *   <li>自动填充：在插入和更新时自动填充时间字段</li>
 * </ul>
 *
 * <h3>分页插件说明</h3>
 * <p>使用方式：在 Mapper 方法中传入 Page 对象即可自动分页</p>
 * <pre>
 * // 示例：查询第1页，每页10条
 * Page&lt;User&gt; page = new Page&lt;&gt;(1, 10);
 * userMapper.selectPage(page, null);
 * // 获取结果：page.getRecords() 返回数据列表
 * // 获取总数：page.getTotal() 返回总记录数
 * </pre>
 *
 * <h3>自动填充说明</h3>
 * <p>需要在实体类字段上添加注解：</p>
 * <pre>
 * &#64;TableField(fill = FieldFill.INSERT)
 * private LocalDateTime createdAt;
 *
 * &#64;TableField(fill = FieldFill.INSERT_UPDATE)
 * private LocalDateTime updatedAt;
 * </pre>
 *
 * @author JTP
 * @since 2026-04-01
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis Plus 拦截器配置
     *
     * <p>配置分页插件，支持 MySQL 数据库分页查询。</p>
     *
     * <h4>DbType 参数说明</h4>
     * <ul>
     *   <li>DbType.MYSQL: MySQL 数据库</li>
     *   <li>DbType.POSTGRE_SQL: PostgreSQL 数据库</li>
     *   <li>DbType.ORACLE: Oracle 数据库</li>
     *   <li>DbType.SQL_SERVER: SQL Server 数据库</li>
     *   <li>不指定: 自动识别数据库类型</li>
     * </ul>
     *
     * <h4>其他常用插件</h4>
     * <pre>
     * // 乐观锁插件（需要在实体类添加 &#64;Version 注解）
     * interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
     *
     * // 防全表更新删除插件（防止误操作）
     * interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
     * </pre>
     *
     * @return MybatisPlusInterceptor 拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页插件
        // DbType.MYSQL 指定数据库类型，生成分页 SQL 时会使用 MySQL 语法（LIMIT）
        // 如使用其他数据库，修改为对应的 DbType 即可
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }

    /**
     * 字段自动填充处理器
     *
     * <p>实现 MetaObjectHandler 接口，在插入和更新操作时自动填充字段值。</p>
     *
     * <h4>使用场景</h4>
     * <ul>
     *   <li>createdAt（创建时间）：仅在插入时填充</li>
     *   <li>updatedAt（更新时间）：插入和更新时都填充</li>
     *   <li>createdBy（创建人）：插入时填充当前用户ID</li>
     *   <li>updatedBy（更新人）：更新时填充当前用户ID</li>
     * </ul>
     *
     * <h4>注意事项</h4>
     * <ul>
     *   <li>实体类字段需要添加 &#64;TableField(fill = FieldFill.INSERT) 注解</li>
     *   <li>strictInsertFill/strictUpdateFill：严格模式，字段为 null 时才填充</li>
     *   <li>fillStrategy：智能模式，根据字段名和类型自动判断</li>
     * </ul>
     *
     * @return MetaObjectHandler 自动填充处理器实例
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {

            /**
             * 插入时自动填充
             *
             * <p>当执行 INSERT 操作时，自动填充以下字段：</p>
             * <ul>
             *   <li>createdAt: 设置为当前时间</li>
             *   <li>updatedAt: 设置为当前时间</li>
             * </ul>
             *
             * @param metaObject 元对象，包含实体类字段信息
             */
            @Override
            public void insertFill(MetaObject metaObject) {
                // strictInsertFill: 严格填充，仅在字段为 null 时填充
                // 参数说明：元对象、字段名、字段类型、填充值
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }

            /**
             * 更新时自动填充
             *
             * <p>当执行 UPDATE 操作时，自动填充以下字段：</p>
             * <ul>
             *   <li>updatedAt: 更新为当前时间</li>
             * </ul>
             *
             * @param metaObject 元对象，包含实体类字段信息
             */
            @Override
            public void updateFill(MetaObject metaObject) {
                // strictUpdateFill: 严格填充，仅在字段为 null 时填充
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}