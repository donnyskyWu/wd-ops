package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import lombok.Data;

@Data
public class IpGroupMemberUpdateReq {

    @InDict("dict_position")
    private String position;

    private Boolean isLeader;
}
