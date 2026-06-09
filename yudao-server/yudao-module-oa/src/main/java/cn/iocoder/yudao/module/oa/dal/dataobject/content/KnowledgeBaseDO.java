package cn.iocoder.yudao.module.oa.dal.dataobject.content;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_knowledge_base")
public class KnowledgeBaseDO extends TenantBaseDO {

    private String title;
    private String content;
    private String category;
    private String tags;
    private Integer isPublic;
    private Integer status;
}
