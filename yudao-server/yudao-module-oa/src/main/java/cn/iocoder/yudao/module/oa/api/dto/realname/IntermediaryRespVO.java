package cn.iocoder.yudao.module.oa.api.dto.realname;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class IntermediaryRespVO {

    private Long id;
    private Long realnameId;
    private String intermediaryName;
    private String intermediaryPhoneMasked;
    private String intermediaryWechat;
    private String relationType;
    /** 数值或脱敏占位 **** */
    private String commissionRateDisplay;
    private BigDecimal commissionRate;
    private String remark;
    private LocalDateTime createTime;
}
