package cn.iocoder.yudao.module.oa.api.dto.config;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CollectConfigUpdateReq {

    @NotNull
    private Long id;

    @Size(max = 128)
    private String configName;

    @Size(max = 32)
    private String subType;

    private String platformType;

    private Long accountId;

    @InDict("dict_collect_frequency")
    private String collectFrequency;

    @InDict("dict_collect_method")
    private String collectMethod;

    @Size(max = 512)
    private String apiUrl;

    @Size(max = 512)
    private String apiKey;

    @Size(max = 16)
    private String requestMethod;

    private String requestParams;
    private String responseMapping;
    private String collectFields;

    @InDict("dict_config_status")
    private String status;

    @Size(max = 512)
    private String remark;
}
