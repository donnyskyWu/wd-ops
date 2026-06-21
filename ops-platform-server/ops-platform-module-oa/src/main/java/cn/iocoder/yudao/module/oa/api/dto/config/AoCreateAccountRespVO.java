package cn.iocoder.yudao.module.oa.api.dto.config;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AoCreateAccountRespVO {

    private Long id;
    private Long aocreateApiId;
    private String accountName;
    private String aochuangAccountId;
    private String status;
    private String connStatus;
    private LocalDateTime lastDeviceSyncAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
