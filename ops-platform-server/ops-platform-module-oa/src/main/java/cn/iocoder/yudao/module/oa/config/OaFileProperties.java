package cn.iocoder.yudao.module.oa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oa.file")
public class OaFileProperties {

    /** 本地文件根目录（ADR-001） */
    private String uploadDir = "./data/uploads";

    /** 单文件大小上限（字节），默认 50MB */
    private long maxFileSize = 50L * 1024 * 1024;
}
