package cn.iocoder.yudao.module.oa.dal.dataobject.content;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_layout_import_job")
public class LayoutImportJobDO extends TenantBaseDO {

    private String sourceType;
    private String sourceUrl;
    private String status;
    private String previewLayoutJson;
    private String previewLayoutSchema;
    private String extractionReport;
    private String suggestedName;
    private String errorMessage;
    private Long creatorUserId;
}
