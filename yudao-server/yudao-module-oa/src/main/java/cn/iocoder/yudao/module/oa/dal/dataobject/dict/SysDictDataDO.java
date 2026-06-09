package cn.iocoder.yudao.module.oa.dal.dataobject.dict;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_dict_data")
public class SysDictDataDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String dictType;
    private String label;
    @TableField("dict_value")
    private String dictValue;
    private Integer sort;
    private String status;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Long tenantId;
}
