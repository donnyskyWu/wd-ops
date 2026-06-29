package cn.iocoder.yudao.module.oa.api.dto.account;

import lombok.Data;

@Data
public class MpFollowerRespVO {

    private Long id;
    private String openid;
    private String nickname;
    private String avatar;
    private String subscribedAt;
    private String syncedAt;
}
