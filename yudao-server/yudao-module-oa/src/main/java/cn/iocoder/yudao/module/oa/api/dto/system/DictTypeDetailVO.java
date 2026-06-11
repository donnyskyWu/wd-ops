package cn.iocoder.yudao.module.oa.api.dto.system;

import lombok.Data;

import java.util.List;

@Data
public class DictTypeDetailVO {

    private String dictType;
    private String dictName;
    private String status;
    private List<DictDataItemVO> items;
}
