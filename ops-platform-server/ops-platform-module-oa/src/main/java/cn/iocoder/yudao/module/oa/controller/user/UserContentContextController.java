package cn.iocoder.yudao.module.oa.controller.user;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.user.UserIpGroupVO;
import cn.iocoder.yudao.module.oa.service.user.UserContentContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/oa/user")
@Validated
@RequiredArgsConstructor
public class UserContentContextController {

    private final UserContentContextService userContentContextService;

    @GetMapping("/ip-groups")
    public CommonResult<List<UserIpGroupVO>> myIpGroups() {
        return CommonResult.success(userContentContextService.listMyIpGroups());
    }
}
