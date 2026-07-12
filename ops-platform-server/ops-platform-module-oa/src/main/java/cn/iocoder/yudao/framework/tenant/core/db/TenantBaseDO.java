package cn.iocoder.yudao.framework.tenant.core.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户基础数据对象抽象类
 * <p>
 * 所有需要支持多租户的实体类都应继承此类，提供租户ID、创建人、
 * 创建时间、更新人、更新时间和逻辑删除标志等通用字段。
 * </p>
 *
 * @author system
 */

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
