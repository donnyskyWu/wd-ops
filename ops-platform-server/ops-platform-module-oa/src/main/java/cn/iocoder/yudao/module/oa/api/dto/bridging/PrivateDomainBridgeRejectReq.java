package cn.iocoder.yudao.module.oa.api.dto.bridging;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PrivateDomainBridgeRejectReq {

    @Size(max = 500)
    private String reason;
}
