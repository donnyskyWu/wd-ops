package cn.iocoder.yudao.module.oa.dal.dataobject.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_tenant")
public class SysTenantDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private LocalDateTime expireTime;
    private Integer maxAccounts;
    private String status;
    private String remark;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
