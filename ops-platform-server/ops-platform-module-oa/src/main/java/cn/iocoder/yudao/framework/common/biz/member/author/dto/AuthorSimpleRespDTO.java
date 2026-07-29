package cn.iocoder.yudao.framework.common.biz.member.author.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * Vendored subset of Football {@code AuthorSimpleRespDTO} (G-MEM-02 read path).
 */
@Data
public class AuthorSimpleRespDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String nickname;

    private String avatarUrl;

    private Integer status;

    private Integer authorLevel;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long tenantId;

    private Integer accessMode;
}
