package cn.iocoder.yudao.module.oa.api.dto.dict;

import lombok.Data;

@Data
public class DictDataRespVO {
    private String dictType;
    private String label;
    private String value;
    private Integer sort;
    private String status;
}
