package cn.iocoder.yudao.framework.common.biz.mp.user;

import cn.iocoder.yudao.framework.common.biz.mp.user.dto.MpAccountDTO;
import cn.iocoder.yudao.framework.common.enums.RpcConstants;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Football mp-server account RPC (vendored subset, G-MP-01).
 * Path prefix {@code /rpc-api/mp/accountInfo} per Football {@code MpAccountInfoApi}.
 */
@FeignClient(name = RpcConstants.MP_NAME, primary = false)
public interface MpAccountInfoApi {

    String PREFIX = RpcConstants.MP_PREFIX + "/accountInfo";

    @PostMapping(PREFIX + "/create")
    CommonResult<Long> createAccount(@RequestBody MpAccountDTO dto);

    @PutMapping(PREFIX + "/update")
    CommonResult<Boolean> updateAccount(@RequestBody MpAccountDTO dto);

    @GetMapping(PREFIX + "/get")
    CommonResult<MpAccountDTO> getAccount(@RequestParam("id") Long id);

    @GetMapping(PREFIX + "/page")
    CommonResult<PageResult<MpAccountDTO>> getAccountPage(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "appId", required = false) String appId,
            @RequestParam(value = "authorId", required = false) Long authorId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "type", required = false) Integer type);

    @GetMapping(PREFIX + "/getMpAccountByAppId")
    CommonResult<MpAccountDTO> getMpAccountByAppId(@RequestParam("appId") String appId);
}
