package cn.iocoder.yudao.module.oa.api.dto.personal;

import lombok.Data;

@Data
public class AochuangFriendRespVO {

    private Long id;
    private Long personalWechatId;
    private String aochuangWechatAccountId;
    private String aochuangFriendId;
    private String wechatId;
    private String alias;
    private String nickname;
    private String avatar;
    private String remark;
    private String syncedAt;
}
