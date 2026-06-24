package cn.iocoder.yudao.module.oa.dal.dataobject.collect;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_douyin_video")
public class DouyinVideoDO extends TenantBaseDO {

    private Long accountId;
    private String videoId;
    private String title;
    private String description;
    private String videoUrl;
    private String coverUrl;
    private Integer duration;
    private LocalDateTime publishedAt;
    private Integer playCount;
    private Integer likeCount;
    private Integer shareCount;
    private Integer commentCount;
    private Integer collectCount;
    private LocalDateTime syncedAt;
    private LocalDateTime statsSyncedAt;
}
