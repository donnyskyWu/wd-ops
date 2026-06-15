package cn.iocoder.yudao.module.oa.api.dto.account;

import lombok.Data;

@Data
public class FanGroupRespVO {

    private Long id;
    private Long accountId;
    private String groupName;
    private Integer memberCount;
    private String createTime;
}
