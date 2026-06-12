package cn.iocoder.yudao.module.oa.dal.dataobject.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_param")
public class SysParamDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String paramName;
    private String paramKey;
    private String paramValue;
    private String paramType;
    private String category;
    private String remark;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
