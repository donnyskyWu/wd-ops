package cn.iocoder.yudao.framework.common.biz.infra.file.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * Football infra-server file create RPC request (vendored subset, G-INF-01).
 * Aligns with {@code football.module.infra.api.file.dto.FileCreateReqDTO}.
 */
@Data
public class FileCreateReqDTO {

    private String name;

    private String directory;

    private String type;

    @NotEmpty(message = "文件内容不能为空")
    private byte[] content;
}
