package cn.iocoder.yudao.module.oa.dal.dataobject.dict;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Football 平台字典数据（shenyu-system.system_dict_data，ADR-050 D4）。
 */
@Data
@TableName("system_dict_data")
public class FootballSystemDictDataDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer sort;
    private String label;
    /** Football 列名 value（非 wd dict_value） */
    private String value;
    private String dictType;
    /** Football: 0=启用 1=停用 */
    private Integer status;
    private String colorType;
    private String cssClass;
    private String remark;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    @TableLogic
    private Boolean deleted;
}
