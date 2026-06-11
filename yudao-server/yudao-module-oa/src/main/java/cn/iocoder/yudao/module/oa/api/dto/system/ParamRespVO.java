package cn.iocoder.yudao.module.oa.api.dto.system;

import lombok.Data;

@Data
public class ParamRespVO {

    private Long id;
    private String paramName;
    private String paramKey;
    private String paramValue;
    private String paramType;
    private String category;
    private String remark;
    private String updateTime;
}
