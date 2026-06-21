package cn.iocoder.yudao.module.oa.api.dto.personal;

import lombok.Data;

@Data
public class PersonalWechatSyncFriendsReq {

    /** 是否全量同步（重置游标从头拉取） */
    private Boolean fullSync;
}
