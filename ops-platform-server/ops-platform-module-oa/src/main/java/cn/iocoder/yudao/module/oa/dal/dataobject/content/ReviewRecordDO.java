package cn.iocoder.yudao.module.oa.dal.dataobject.content;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_review_record")
public class ReviewRecordDO extends TenantBaseDO {

    private Long contentId;
    private String stage;
    private String action;
    private Long reviewerId;
    private String comment;
}
