package cn.iocoder.yudao.framework.tenant.core.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public abstract class TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String creator;

    private LocalDateTime createTime;

    private String updater;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
