package cn.iocoder.yudao.module.oa.dal.dataobject.analytics;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_funnel_step")
public class FunnelStepDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long funnelId;
    private Integer stepOrder;
    private String eventCode;
    private String stepName;

    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
