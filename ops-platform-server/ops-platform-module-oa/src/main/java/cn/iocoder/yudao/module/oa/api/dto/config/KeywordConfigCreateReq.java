package cn.iocoder.yudao.module.oa.api.dto.config;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KeywordConfigCreateReq {

    @NotBlank
    @InDict("dict_platform_type")
    private String platform;

    @NotBlank
    @Size(max = 100)
    private String keyword;

    @InDict("dict_match_type")
    private String matchType;

    @InDict("dict_config_status")
    private String status;
}
