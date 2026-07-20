package cn.iocoder.yudao.module.oa.dal.dataobject.aicontent;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_ai_content_conversation")
public class AiContentConversationDO extends TenantBaseDO {

    private Long userId;
    private String scopeKey;
    private Long contentId;
    private Long authorId;
    private String conversationJson;
    private Integer roundCount;
    private String sourceSessionId;
}
