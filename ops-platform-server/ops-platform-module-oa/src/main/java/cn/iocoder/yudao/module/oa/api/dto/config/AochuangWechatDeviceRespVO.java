package cn.iocoder.yudao.module.oa.api.dto.config;

import lombok.Data;

@Data
public class AochuangWechatDeviceRespVO {

    private String wechatAccountId;
    private String wechatId;
    private String alias;
    private String nickname;
    private String avatar;
    private Boolean isAlive;
}
