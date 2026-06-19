package cn.iocoder.yudao.module.oa.api.dto.content;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TypesettingRuleUpdateReq {

    @NotNull
    private Long id;
    private String name;
    private String description;
    private Object ruleConfig;
    private Integer sort;
    private String status;
}
