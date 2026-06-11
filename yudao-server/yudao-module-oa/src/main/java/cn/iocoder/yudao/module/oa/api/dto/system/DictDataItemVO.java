package cn.iocoder.yudao.module.oa.api.dto.system;

import lombok.Data;

@Data
public class DictDataItemVO {

    private Long id;
    private String dictLabel;
    private String value;
    private Integer sort;
    private String status;
    private String colorType;
    private String remark;
}
