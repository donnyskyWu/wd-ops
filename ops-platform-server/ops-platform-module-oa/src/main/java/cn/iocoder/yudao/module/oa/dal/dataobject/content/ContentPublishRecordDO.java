package cn.iocoder.yudao.module.oa.dal.dataobject.content;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_content_publish_record")
public class ContentPublishRecordDO extends TenantBaseDO {

    private Long contentId;
    private Long accountId;
    private String platformType;
    /** SUCCESS / FAILED */
    private String status;
    private String externalId;
    private String errorMessage;
    private LocalDateTime publishedAt;
}
