package cn.iocoder.yudao.module.oa.dal.dataobject.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role_permission")
public class SysRolePermissionDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roleId;
    private Long permissionId;
    private String creator;
    private LocalDateTime createTime;
}
