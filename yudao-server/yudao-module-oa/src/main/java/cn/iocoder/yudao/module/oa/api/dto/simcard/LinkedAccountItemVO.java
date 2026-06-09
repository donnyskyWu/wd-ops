package cn.iocoder.yudao.module.oa.api.dto.simcard;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LinkedAccountItemVO {

    private String platformType;
    private String platformLabel;
    private String accountName;
    private String accountId;
    private String status;
    private LocalDateTime linkedAt;
}
