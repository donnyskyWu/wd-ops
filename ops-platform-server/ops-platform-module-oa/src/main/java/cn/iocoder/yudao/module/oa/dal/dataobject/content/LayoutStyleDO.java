package cn.iocoder.yudao.module.oa.dal.dataobject.content;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_layout_style")
public class LayoutStyleDO extends TenantBaseDO {

    private String styleCode;
    private String name;
    private String category;
    private String tags;
    private String htmlSnippet;
    private Long thumbnailFileId;
    private Integer sort;
    private String status;
}
