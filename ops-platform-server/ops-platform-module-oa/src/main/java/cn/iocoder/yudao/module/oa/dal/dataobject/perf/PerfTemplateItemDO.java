package cn.iocoder.yudao.module.oa.dal.dataobject.perf;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oa_perf_template_item")
public class PerfTemplateItemDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Long metricId;
    private BigDecimal weight;
    private String calcRule;
    private String scoreStandardJson;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
