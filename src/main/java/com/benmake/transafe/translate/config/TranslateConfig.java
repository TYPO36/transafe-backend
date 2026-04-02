package com.benmake.transafe.translate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 翻译服务配置
 *
 * @author JTP
 * @date 2026-04-02
 */
@Configuration
public class TranslateConfig {

    /**
     * RestTemplate Bean（用于翻译API调用）
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}