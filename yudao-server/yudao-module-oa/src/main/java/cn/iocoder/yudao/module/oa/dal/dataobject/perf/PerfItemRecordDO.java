package cn.iocoder.yudao.module.oa.dal.dataobject.perf;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oa_perf_item_record")
public class PerfItemRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    private Long metricId;
    private BigDecimal metricValue;
    private BigDecimal score;
    private BigDecimal manualAdjustment;
    private BigDecimal finalScore;
    private String remark;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
