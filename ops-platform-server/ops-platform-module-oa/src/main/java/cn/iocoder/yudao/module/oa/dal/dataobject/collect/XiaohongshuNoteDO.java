package cn.iocoder.yudao.module.oa.dal.dataobject.collect;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_xiaohongshu_note")
public class XiaohongshuNoteDO extends TenantBaseDO {

    private Long accountId;
    private String noteId;
    private String xsecToken;
    private String title;
    private String description;
    private String noteUrl;
    private String coverUrl;
    private LocalDateTime publishedAt;
    private Integer playCount;
    private Integer likeCount;
    private Integer shareCount;
    private Integer commentCount;
    private Integer collectCount;
    private LocalDateTime syncedAt;
    private LocalDateTime statsSyncedAt;
}
