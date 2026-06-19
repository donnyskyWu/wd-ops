package cn.iocoder.yudao.module.oa.api.dto.content;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TypesettingRuleCreateReq {

    @NotBlank
    private String ruleCode;
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private Object ruleConfig;
    private Integer sort;
    private String status;
}
