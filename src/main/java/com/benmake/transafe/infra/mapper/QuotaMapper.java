package com.benmake.transafe.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.benmake.transafe.quota.entity.QuotaEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 配额 Mapper
 *
 * @author JTP
 * @date 2026-04-01
 */
@Mapper
public interface QuotaMapper extends BaseMapper<QuotaEntity> {

    /**
     * 根据用户ID查询配额
     *
     * @param userId 用户ID
     * @return 配额实体
     */
    @Select("SELECT * FROM quota WHERE user_id = #{userId}")
    Optional<QuotaEntity> findByUserId(@Param("userId") Long userId);
}