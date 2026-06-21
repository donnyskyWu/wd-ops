package cn.iocoder.yudao.module.oa.api.dto.config;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AoCreateAccountUpdateReq {

    @NotNull
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String accountName;

    @NotBlank
    @Size(max = 64)
    private String aochuangAccountId;

    @InDict("dict_config_status")
    private String status;
}
