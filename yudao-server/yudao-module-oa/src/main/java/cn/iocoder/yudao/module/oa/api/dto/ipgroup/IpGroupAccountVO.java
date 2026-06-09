package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IpGroupAccountVO {

    private Long accountId;
    private String accountName;
    private String platform;
    private String platformText;
    private Long followerCount;
    private Integer contentCount;
    private LocalDateTime boundAt;
}
