package cn.iocoder.yudao.module.oa.dal.dataobject.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String level;
    private String content;
    private String method;
    private String requestParams;
    private String responseBody;
    private String ip;
    private String status;
    private LocalDateTime createTime;
}
