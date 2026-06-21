package cn.iocoder.yudao.module.oa.service.config.aochuang;

import lombok.Data;

@Data
public class AochuangWechatAccountDTO {

    private String wechatAccountId;
    private String wechatId;
    private String alias;
    private String nickname;
    private String avatar;
    private Boolean isAlive;
}
