package cn.iocoder.yudao.module.oa.api.dto.system;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantRespVO {

    private Long id;
    private String tenantName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private LocalDateTime expireTime;
    private Integer maxAccounts;
    private Long accountCount;
    private String status;
    private String remark;
    private LocalDateTime createTime;
}
