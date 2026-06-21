package cn.iocoder.yudao.module.oa.api.dto.personal;

import lombok.Data;

@Data
public class PersonalWechatSyncFriendsRespVO {

    private Integer syncedCount;
    private Integer createdCount;
    private Integer updatedCount;
    /** 是否已完成全量分页（hasMore=false） */
    private Boolean completed;
}
