package cn.iocoder.yudao.module.oa.dal.dataobject.auth;

import lombok.Data;

@Data
public class FootballSystemUserDO {

    private Long id;
    private Long tenantId;
    private String username;
    private String nickname;
    private String email;
    private Integer status;
}
