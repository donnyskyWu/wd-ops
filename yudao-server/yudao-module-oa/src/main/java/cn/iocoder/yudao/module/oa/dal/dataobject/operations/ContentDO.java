package cn.iocoder.yudao.module.oa.dal.dataobject.operations;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_content")
public class ContentDO extends TenantBaseDO {

    private Long accountId;
    private String title;
    private String platformType;
    private String contentType;
    private LocalDateTime publishTime;
    private Long readCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer forwardCount;
    private Integer isHit;
    private String dataSource;
    private String status;
}
