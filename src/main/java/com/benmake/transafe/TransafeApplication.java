package com.benmake.transafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Transafe 业务模块启动类
 *
 * @author TYPO
 * @since 2026-03-30
 */
@SpringBootApplication
@EnableFeignClients
public class TransafeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransafeApplication.class, args);
    }
}
