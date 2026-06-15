package cn.iocoder.yudao.module.oa.api.dto.account;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WechatCertRenewalCreateReq {

    @NotNull
    private Long accountId;

    @NotNull
    private LocalDateTime renewalTime;

    private Long renewerUserId;

    @DecimalMin(value = "0.01", message = "续费金额必须大于 0")
    private BigDecimal renewalAmount;
}
