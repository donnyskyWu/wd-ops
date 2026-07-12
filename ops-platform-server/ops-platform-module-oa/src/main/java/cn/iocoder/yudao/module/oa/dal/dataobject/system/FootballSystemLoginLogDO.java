package cn.iocoder.yudao.module.oa.dal.dataobject.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Football 登录日志（shenyu-system.system_login_log，ADR-050 D6）。
 */
@Data
@TableName("system_login_log")
public class FootballSystemLoginLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long logType;
    private String traceId;
    private Long userId;
    private Integer userType;
    private String username;
    /** Football: 0=成功，非 0=失败 */
    private Integer result;
    private String userIp;
    private String userAgent;
    private Long tenantId;
    private LocalDateTime createTime;
    @TableLogic
    private Boolean deleted;
}
