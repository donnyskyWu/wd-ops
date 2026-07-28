package cn.iocoder.yudao.framework.common.biz.mp.user.dto;

import lombok.Data;

/**
 * Vendored subset of Football {@code MpAccountDTO} (G-MP-01).
 */
@Data
public class MpAccountDTO {

    private Long id;
    private String name;
    private String account;
    private String appId;
    private String appSecret;
    private String token;
    private String aesKey;
    private String remark;
    private Integer status;
    private Long bindAuthorId;
    private Integer type;
    private Integer isPrimary;
    private Long targetGroupId;
    private Integer pushLimitNum;
}
