package cn.iocoder.yudao.module.oa.api.dto.system;

import lombok.Data;

@Data
public class DictAdminRowVO {

    private Long id;
    private Long typeId;
    private String dictName;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer sort;
    private String status;
    private String colorType;
    private String remark;
}
