package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IpGroupMemberVO {

    private Long memberId;
    private Long userId;
    private String userName;
    private String position;
    private String positionText;
    private Boolean isLeader;
    private LocalDateTime joinTime;
}
