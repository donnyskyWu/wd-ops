package cn.iocoder.yudao.module.oa.api.dto.collect;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CollectTaskCreateReq {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @InDict("dict_platform_type")
    private String platformType;

    @NotNull
    private Long accountId;

    @InDict("dict_collect_method")
    private String method;

    @InDict("dict_collect_source")
    private String source;

    @InDict("dict_collect_data_type")
    private String dataType;

    @NotBlank
    @InDict("dict_collect_frequency")
    private String frequency;

    @NotBlank
    @Size(max = 64)
    private String cron;

    private String apiConfig;

    @InDict("dict_collect_status")
    private String status;
}
