package cn.iocoder.yudao.module.oa.api.dto.analytics;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomQueryCreateReq {

    @NotBlank
    private String queryName;

    @InDict("dict_query_status")
    private String status;

    @NotBlank
    private String sqlText;

    private String params;
}
