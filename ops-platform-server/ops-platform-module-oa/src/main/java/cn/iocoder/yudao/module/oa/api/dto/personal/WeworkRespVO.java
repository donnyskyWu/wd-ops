package cn.iocoder.yudao.module.oa.api.dto.personal;

import lombok.Data;

@Data
public class WeworkRespVO {

    private Long id;
    private String accountName;
    private String corpId;
    private String agentId;
    private String secret;
    private String status;
    private String connStatus;
    private String lastHealthCheckAt;
    private String createTime;
}
