package cn.iocoder.yudao.module.oa.api.dto.personal;

import lombok.Data;

@Data
public class PersonalWechatSyncMessagesRespVO {

    private Integer syncedCount;
    private Integer createdCount;
    private Integer updatedCount;
    /** 日统计更新天数 */
    private Integer dailyStatsDays;
    /** 是否已完成全量分页（hasMore=false） */
    private Boolean completed;
}
