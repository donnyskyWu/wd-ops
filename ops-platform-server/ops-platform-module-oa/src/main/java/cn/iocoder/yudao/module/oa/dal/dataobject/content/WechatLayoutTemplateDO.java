package cn.iocoder.yudao.module.oa.dal.dataobject.content;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_wechat_layout_template")
public class WechatLayoutTemplateDO extends TenantBaseDO {

    private String templateName;
    private String description;
    private String contentType;
    private String documentType;
    private String layoutJson;
    private String layoutSchema;
    private Integer schemaVersion;
    private String styleCss;
    private String layoutHtml;
    private String previewHtml;
    private String thumbnailUrl;
    private String sourceType;
    private String sourceUrl;
    private String status;
    private Long creatorUserId;
}
