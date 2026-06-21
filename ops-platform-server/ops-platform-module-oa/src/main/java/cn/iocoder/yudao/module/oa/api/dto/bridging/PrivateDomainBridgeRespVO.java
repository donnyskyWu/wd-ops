package cn.iocoder.yudao.module.oa.api.dto.bridging;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PrivateDomainBridgeRespVO {

    private Long id;
    private String sourceType;
    private Long sourceId;
    private String sourceLabel;
    private String targetType;
    private Long targetId;
    private String targetLabel;
    private String matchMethod;
    private BigDecimal confidence;
    private String matchEvidenceJson;
    private String reviewStatus;
    private String linkedBy;
    private String linkedAt;
    private String createTime;
}
