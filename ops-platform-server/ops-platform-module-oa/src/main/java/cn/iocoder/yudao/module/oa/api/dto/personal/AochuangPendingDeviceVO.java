package cn.iocoder.yudao.module.oa.api.dto.personal;

import lombok.Data;

@Data
public class AochuangPendingDeviceVO {

    private String aochuangWechatAccountId;
    private String wechatId;
    private String alias;
    private String nickname;
    private String avatar;
    private Boolean isAlive;
    private Long aochuangAccountRefId;
    private String aochuangAccountName;
    private Long suggestedPersonalWechatId;
    private Double fuzzyScore;
}
