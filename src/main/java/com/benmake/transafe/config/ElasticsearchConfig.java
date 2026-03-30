package com.benmake.transafe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch 配置
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Configuration
@ConfigurationProperties(prefix = "spring.elasticsearch")
public class ElasticsearchConfig {
    // Elasticsearch 配置由 Spring Boot 自动配置
}