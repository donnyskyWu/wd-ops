package cn.iocoder.yudao.module.oa.dal.dataobject.collect;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_wechat_mp_article")
public class WechatMpArticleDO extends TenantBaseDO {

    private Long accountId;
    private String articleId;
    private String title;
    private String url;
    private String coverUrl;
    private LocalDateTime publishedAt;
    private Integer readCount;
    private Integer likeCount;
    private Integer shareCount;
    private String contentText;
    private LocalDateTime syncedAt;
    private LocalDateTime statsSyncedAt;
    private LocalDateTime contentSyncedAt;
}
