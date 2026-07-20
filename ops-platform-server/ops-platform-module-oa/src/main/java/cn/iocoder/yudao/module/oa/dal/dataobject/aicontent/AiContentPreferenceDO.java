package cn.iocoder.yudao.module.oa.dal.dataobject.aicontent;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_ai_content_preference")
public class AiContentPreferenceDO extends TenantBaseDO {

    private Long userId;
    private Long authorId;
    private String summaryText;
    private String dimensionsJson;
    private String sourceSessionId;
    private Long contentId;
    private Integer isUpdatedByUser;
}
