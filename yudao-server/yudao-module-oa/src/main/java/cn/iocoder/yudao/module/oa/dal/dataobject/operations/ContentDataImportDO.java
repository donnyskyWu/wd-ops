package cn.iocoder.yudao.module.oa.dal.dataobject.operations;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_content_data_import")
public class ContentDataImportDO extends TenantBaseDO {

    private Long contentId;
    private LocalDate statDate;
    private String importType;
    private Long readCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer forwardCount;
    private Integer followerChange;
    private Integer reviewStatus;
    private String remark;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private Long submitterId;
}
