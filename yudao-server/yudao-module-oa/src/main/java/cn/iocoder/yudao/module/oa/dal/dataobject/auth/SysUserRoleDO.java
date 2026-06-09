package cn.iocoder.yudao.module.oa.dal.dataobject.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_role")
public class SysUserRoleDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long roleId;
    private String creator;
    private LocalDateTime createTime;
}
