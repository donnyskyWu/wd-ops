package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IpGroupAnchorVO {

    private Long relId;
    private Long anchorUserId;
    private String anchorUserName;
    private String anchorType;
    private LocalDateTime boundAt;
}
