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
}
