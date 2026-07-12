package cn.iocoder.yudao.module.oa.dal.dataobject.dict;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Football 平台字典类型（shenyu-system.system_dict_type，ADR-050 D4）。
 */
@Data
@TableName("system_dict_type")
public class FootballSystemDictTypeDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    /** Football: 0=启用 1=停用 */
    private Integer status;
    private String remark;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    @TableLogic
    private Boolean deleted;
}
