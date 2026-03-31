package com.benmake.transafe.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件存储配置
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "file-storage")
public class FileStorageConfig {

    /**
     * 存储类型：local-本地存储
     */
    private String type = "local";

    /**
     * 本地存储根目录
     */
    private String localPath = "./files";

    /**
     * 存储基础路径
     */
    private Path getBasePath() {
        return Paths.get(localPath).toAbsolutePath().normalize();
    }

    /**
     * 获取用户的文件存储路径
     */
    public Path getUserFilePath(Long userId) {
        return getBasePath().resolve("user_" + userId);
    }

    /**
     * 获取文件的完整路径
     */
    public Path getFilePath(Long userId, String fileId) {
        return getUserFilePath(userId).resolve(fileId);
    }
}
