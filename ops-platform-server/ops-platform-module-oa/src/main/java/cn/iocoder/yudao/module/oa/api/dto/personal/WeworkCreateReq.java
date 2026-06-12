package cn.iocoder.yudao.module.oa.api.dto.personal;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WeworkCreateReq {

    @NotBlank
    private String accountName;
    @NotBlank
    private String corpId;
    @NotBlank
    private String agentId;
    @NotBlank
    private String secret;
    private String status;
}
