package cn.iocoder.yudao.framework.common.biz.mp.user;

import cn.iocoder.yudao.framework.common.biz.mp.user.dto.MpUserDTO;
import cn.iocoder.yudao.framework.common.enums.RpcConstants;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Football mp-server MpUser read RPC (vendored subset, G-MP-01 粉丝分页).
 * Path prefix {@code /rpc-api/mp/mpUser} per Football {@code MpUserApi}.
 */
@FeignClient(contextId = "mpUserApi", name = RpcConstants.MP_NAME, primary = false)
public interface MpUserApi {

    String PREFIX = RpcConstants.MP_PREFIX + "/mpUser";

    @GetMapping(PREFIX + "/getUserPageByAccount")
    CommonResult<PageResult<MpUserDTO>> getUserPageByAccount(
            @RequestParam("accountId") Long accountId,
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "subscribeStatus", required = false) Integer subscribeStatus);
}
