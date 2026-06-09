package cn.iocoder.yudao.module.oa.dal.dataobject.content;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_production_content")
public class ProductionContentDO extends TenantBaseDO {

    private String title;
    private String body;
    private String coverImage;
    private Long creatorUserId;
    private Long accountId;
    private String platformType;
    private String contentType;
    private String status;
    private Integer aiGenerated;
}
