package cn.iocoder.yudao.framework.common.biz.mp.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Vendored subset of Football {@code MpUserDTO} (G-MP-01 粉丝分页读).
 */
@Data
public class MpUserDTO {

    private Long id;
    private String openid;
    private String unionId;
    private Integer subscribeStatus;
    private LocalDateTime subscribeTime;
    private LocalDateTime unsubscribeTime;
    private String nickname;
    private String headImageUrl;
    private Long accountId;
    private String appId;
    private LocalDateTime updateTime;
}
