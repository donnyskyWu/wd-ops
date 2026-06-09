package cn.iocoder.yudao.module.oa.controller.health;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUserContext;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/admin-api/oa")
public class HelloController {

    @GetMapping("/hello")
    @AuditLog(module = "health", action = "hello")
    public CommonResult<HelloRespVO> hello() {
        LoginUser user = LoginUserContext.getRequired();
        HelloRespVO resp = new HelloRespVO();
        resp.setUserId(user.getUserId());
        resp.setTenantId(user.getTenantId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setAuthorities(user.getAuthorities());
        resp.setMessage("GATE-S0 HelloWorld OK");
        return CommonResult.success(resp);
    }

    @Data
    public static class HelloRespVO {
        private Long userId;
        private Long tenantId;
        private String username;
        private String nickname;
        private Set<String> authorities;
        private String message;
    }
}
