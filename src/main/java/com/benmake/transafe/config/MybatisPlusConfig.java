package com.benmake.transafe.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
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
     * <p>配置分页插件和乐观锁插件，支持 MySQL 数据库。</p>
     *
     * <h4>已配置插件</h4>
     * <ul>
     *   <li>分页插件：自动生成分页 SQL</li>
     *   <li>乐观锁插件：通过 &#64;Version 注解实现乐观锁</li>
     * </ul>
     *
     * @return MybatisPlusInterceptor 拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        // 添加乐观锁插件（实体类需要添加 @Version 注解字段）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }

    /**
     * 字段自动填充处理器
     *
     * <p>实现 MetaObjectHandler 接口，在插入和更新操作时自动填充字段值。</p>
     *
     * <h4>自动填充字段</h4>
     * <ul>
     *   <li>createdAt（创建时间）：仅在插入时填充</li>
     *   <li>updatedAt（更新时间）：插入和更新时都填充</li>
     *   <li>deleted（逻辑删除）：插入时默认填充为 0</li>
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
             * @param metaObject 元对象，包含实体类字段信息
             */
            @Override
            public void insertFill(MetaObject metaObject) {
                // 时间字段自动填充
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                // 逻辑删除字段默认值填充
                this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
            }

            /**
             * 更新时自动填充
             *
             * @param metaObject 元对象，包含实体类字段信息
             */
            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}