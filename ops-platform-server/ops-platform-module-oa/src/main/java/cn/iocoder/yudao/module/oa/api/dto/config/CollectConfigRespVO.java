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
    /** M4 oa_account.account_name，accountId 关联时填充 */
    private String accountName;
    private String accountIdentifier;
    private String appId;
    private String appSecretMasked;
    private String cookie;
    private String authTokenMasked;
    private String fieldMapping;
    private Boolean isLive;
    private String dbHost;
    private Integer dbPort;
    private String dbName;
    private String dbUsername;
    private String dbPasswordMasked;
    private String tableName;
    private String syncMode;
    private String connStatus;
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
    private LocalDateTime updateTime;
}
