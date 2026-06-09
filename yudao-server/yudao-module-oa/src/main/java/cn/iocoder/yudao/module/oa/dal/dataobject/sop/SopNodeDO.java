package cn.iocoder.yudao.module.oa.dal.dataobject.sop;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_sop_node")
public class SopNodeDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private String nodeName;
    private Integer nodeOrder;
    private String executorRole;
    private Integer needReview;
    private String reviewerRole;
    private String predecessorsJson;
    private String parallelGroup;
    private Integer slaHours;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
