package cn.iocoder.yudao.framework.common.biz.system.user.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * Vendored subset of Football {@code AdminUserRespDTO} for G-SYS-01 Feign dual-run (C-WP2).
 */
@Data
public class AdminUserRespDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long tenantId;

    private String nickname;

    /** {@link cn.iocoder.yudao.framework.common.enums.CommonStatusEnum} */
    private Integer status;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    /** 钉钉 userId（C-WP6 G-DING bridge） */
    private String dingtalkUserId;
}
