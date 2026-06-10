package cn.iocoder.yudao.module.oa.dal.dataobject.analytics;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 内容按日统计 DO。
 * 注：oa_content_daily 表无 creator/updater/update_time 字段，故不继承 TenantBaseDO，
 * 只保留 tenant_id + create_time（与 oa_follower_daily 对齐）。
 */
@Data
@TableName("oa_content_daily")
public class ContentDailyDO {
    private Long id;
    private Long tenantId;
    private Long contentId;
    private LocalDate statDate;
    private Long readCount;
    private Long playCount;
    private String creator;
    private LocalDateTime createTime;
    private Short deleted;
}
