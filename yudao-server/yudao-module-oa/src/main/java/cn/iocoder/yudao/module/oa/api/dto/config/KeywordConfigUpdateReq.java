package cn.iocoder.yudao.module.oa.api.dto.config;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KeywordConfigUpdateReq {

    @NotNull
    private Long id;

    @InDict("dict_platform_type")
    private String platform;

    @Size(max = 100)
    private String keyword;

    @InDict("dict_match_type")
    private String matchType;

    @InDict("dict_config_status")
    private String status;
}
