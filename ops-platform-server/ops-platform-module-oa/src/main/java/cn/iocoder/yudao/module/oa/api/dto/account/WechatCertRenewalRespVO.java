package cn.iocoder.yudao.module.oa.api.dto.account;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WechatCertRenewalRespVO {

    private Long id;
    private Long accountId;
    private String renewalTime;
    private Long renewerUserId;
    private String renewerName;
    private BigDecimal renewalAmount;
    private String createTime;
}
