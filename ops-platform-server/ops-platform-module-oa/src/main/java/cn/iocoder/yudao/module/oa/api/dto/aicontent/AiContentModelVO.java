package cn.iocoder.yudao.module.oa.api.dto.aicontent;

import lombok.Data;

@Data
public class AiContentModelVO {

    private Long id;
    private String name;
    private String icon;
    private String status;
    private String description;
    private Boolean isDefault;
}
