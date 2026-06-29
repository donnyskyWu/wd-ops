package cn.iocoder.yudao.module.oa.api.dto.account;

import lombok.Data;

@Data
public class DouyinFollowerRespVO {

    private Long id;
    private String followerId;
    private String nickname;
    private String avatar;
    private String followedAt;
    private String syncedAt;
}
