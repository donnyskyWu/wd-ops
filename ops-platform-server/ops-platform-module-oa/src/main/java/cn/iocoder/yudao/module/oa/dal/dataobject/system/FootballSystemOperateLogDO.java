package cn.iocoder.yudao.module.oa.dal.dataobject.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Football 操作日志 V2（shenyu-system.system_operate_log，ADR-050 D6）。
 */
@Data
@TableName("system_operate_log")
public class FootballSystemOperateLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;
    private Long userId;
    private Integer userType;
    /** Football module / biz type */
    private String type;
    private String subType;
    private Long bizId;
    /** Human-readable action description */
    private String action;
    private Boolean success;
    private String extra;
    private String requestMethod;
    private String requestUrl;
    private String userIp;
    private String userAgent;
    private Long tenantId;
    private LocalDateTime createTime;
    @TableLogic
    private Boolean deleted;
}
