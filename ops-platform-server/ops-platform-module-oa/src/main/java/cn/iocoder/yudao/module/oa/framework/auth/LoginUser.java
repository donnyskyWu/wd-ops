package cn.iocoder.yudao.module.oa.framework.auth;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class LoginUser {

    private Long userId;
    private Long tenantId;
    private String username;
    private String nickname;
    private String email;
    private Set<String> authorities;
    private String dataScope;
    private Long ipGroupId;
    /** oa_ip_group_member 多组（登录时预计算） */
    private Set<Long> memberIpGroupIds;
    /** 组长管辖组（登录时预计算） */
    private Set<Long> ledIpGroupIds;
    /** ledIpGroupIds 非空 */
    private Boolean ipGroupLeader;
}
