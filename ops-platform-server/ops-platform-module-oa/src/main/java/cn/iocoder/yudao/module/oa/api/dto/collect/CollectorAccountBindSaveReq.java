package cn.iocoder.yudao.module.oa.api.dto.collect;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CollectorAccountBindSaveReq {

    @NotNull
    private Long oaAccountId;

    @NotBlank
    @Size(max = 64)
    private String collectorAccountId;

    @NotBlank
    @InDict("dict_platform_type")
    private String platformType;

    @InDict("dict_collector_bind_status")
    private String bindStatus;

    @InDict("dict_conn_status")
    private String connStatus;
}
