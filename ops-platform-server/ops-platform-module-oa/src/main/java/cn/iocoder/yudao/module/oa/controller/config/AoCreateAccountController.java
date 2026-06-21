package cn.iocoder.yudao.module.oa.controller.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.AochuangWechatDeviceRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateAccountCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateAccountRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateAccountTestConnectionRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateAccountUpdateReq;
import cn.iocoder.yudao.module.oa.service.config.AoCreateAccountService;
import cn.iocoder.yudao.module.oa.service.collect.aochuang.AochuangAdapter;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangWechatAccountDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/config/internal-collect/aocreate/accounts")
@Validated
@RequiredArgsConstructor
public class AoCreateAccountController {

    private final AoCreateAccountService aoCreateAccountService;
    private final AochuangAdapter aochuangAdapter;

    @GetMapping("/list")
    public CommonResult<PageResult<AoCreateAccountRespVO>> list(
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(aoCreateAccountService.list(accountName, status, pageNo, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody AoCreateAccountCreateReq req) {
        return CommonResult.success(aoCreateAccountService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody AoCreateAccountUpdateReq req) {
        aoCreateAccountService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        aoCreateAccountService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/test-connection")
    public CommonResult<AoCreateAccountTestConnectionRespVO> testConnection(@PathVariable Long id) {
        return CommonResult.success(aoCreateAccountService.testConnection(id));
    }

    @GetMapping("/{id}/wechat-devices")
    public CommonResult<java.util.List<AochuangWechatDeviceRespVO>> listWechatDevices(@PathVariable Long id) {
        java.util.List<AochuangWechatAccountDTO> devices = aochuangAdapter.listWechatAccounts(id);
        java.util.List<AochuangWechatDeviceRespVO> list = devices.stream().map(d -> {
            AochuangWechatDeviceRespVO vo = new AochuangWechatDeviceRespVO();
            vo.setWechatAccountId(d.getWechatAccountId());
            vo.setWechatId(d.getWechatId());
            vo.setAlias(d.getAlias());
            vo.setNickname(d.getNickname());
            vo.setAvatar(d.getAvatar());
            vo.setIsAlive(d.getIsAlive());
            return vo;
        }).toList();
        return CommonResult.success(list);
    }
}
