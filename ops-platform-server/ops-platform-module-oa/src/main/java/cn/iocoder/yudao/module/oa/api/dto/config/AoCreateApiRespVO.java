package cn.iocoder.yudao.module.oa.api.dto.config;

import lombok.Data;

@Data
public class AoCreateApiRespVO {

    private Long id;
    private String apiUrl;
    private String appId;
    private String appSecretMasked;
    private String tokenMasked;
    private String status;
    private Integer dailyQuota;
    private Integer currentUsage;
}
