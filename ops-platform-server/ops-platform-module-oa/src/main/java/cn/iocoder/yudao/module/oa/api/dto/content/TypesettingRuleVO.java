package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TypesettingRuleVO {

    private Long id;
    private String ruleCode;
    private String name;
    private String description;
    private Object ruleConfig;
    private Integer sort;
    private String status;
    private LocalDateTime updateTime;
}
