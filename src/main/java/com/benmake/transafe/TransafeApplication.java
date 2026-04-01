package com.benmake.transafe;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Transafe 业务模块启动类
 *
 * @author JTP
 * @date 2026-04-01
 */
@SpringBootApplication
@EnableFeignClients
@MapperScan("com.benmake.transafe.infra.mapper")
public class TransafeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransafeApplication.class, args);
    }
}