package cn.iocoder.yudao.module.oa.api.dto.bridging;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PrivateDomainBridgeCreateReq {

    @NotBlank
    @InDict("dict_private_domain_identity_type")
    private String sourceType;

    @NotNull
    private Long sourceId;

    @NotBlank
    @InDict("dict_private_domain_identity_type")
    private String targetType;

    @NotNull
    private Long targetId;

    @InDict("dict_private_domain_match_method")
    private String matchMethod;

    private BigDecimal confidence;

    private String matchEvidenceJson;
}
