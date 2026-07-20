package cn.iocoder.yudao.module.oa.dal.dataobject.aicontent;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_ai_content_session")
public class AiContentSessionDO extends TenantBaseDO {

    private String sessionId;
    private Long userId;
    private String modelKey;
    private Integer roundCount;
    private String lastContent;
    private String contextJson;
}
