package cn.iocoder.yudao.module.oa.dal.dataobject.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_message")
public class SysMessageDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String title;
    private String category;
    private String channel;
    private String receiver;
    private String content;
    private String status;
    private String failReason;
    private LocalDateTime sendTime;
    private LocalDateTime readTime;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
