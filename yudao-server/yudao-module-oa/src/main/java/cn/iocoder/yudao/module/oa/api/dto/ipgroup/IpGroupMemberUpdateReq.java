package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import lombok.Data;

@Data
public class IpGroupMemberUpdateReq {

    private String position;
    private Boolean isLeader;
}
