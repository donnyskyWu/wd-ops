package cn.iocoder.yudao.module.oa.api.dto.personal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WeworkUpdateReq {

    @NotNull
    private Long id;
    private String accountName;
    private String corpId;
    private String agentId;
    private String secret;
    private String status;
}
