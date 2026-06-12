package cn.iocoder.yudao.module.oa.dal.dataobject.perf;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_perf_record")
public class PerfRecordDO extends TenantBaseDO {

    private Long templateId;
    private Long targetUserId;
    private Long ipGroupId;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalScore;
    private String grade;
    private String status;
    private String remark;
}
