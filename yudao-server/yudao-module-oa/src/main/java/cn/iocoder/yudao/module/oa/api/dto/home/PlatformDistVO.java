package cn.iocoder.yudao.module.oa.api.dto.home;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlatformDistVO {
    private String platform;
    private Long count;
    private BigDecimal percentage;
}
