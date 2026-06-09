package cn.iocoder.yudao.module.oa.api.dto.config;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CollectConfigRespVO {

    private Long id;
    private String configName;
    private String subType;
    private String platformType;
    private Long accountId;
    private String collectFrequency;
    private String collectMethod;
    private String apiUrl;
    private String apiKeyMasked;
    private String requestMethod;
    private String requestParams;
    private String responseMapping;
    private String collectFields;
    private String status;
    private String remark;
    private LocalDateTime createTime;
}
