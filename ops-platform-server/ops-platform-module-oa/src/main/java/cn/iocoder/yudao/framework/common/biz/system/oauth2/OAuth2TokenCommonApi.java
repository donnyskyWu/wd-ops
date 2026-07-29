package cn.iocoder.yudao.framework.common.biz.system.oauth2;

import cn.iocoder.yudao.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCheckRespDTO;
import cn.iocoder.yudao.framework.common.enums.RpcConstants;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Football system-server OAuth2 token RPC (vendored subset, D-SYS-03).
 */
@FeignClient(contextId = "oauth2TokenCommonApi", name = RpcConstants.SYSTEM_NAME, primary = false)
public interface OAuth2TokenCommonApi {

    String PREFIX = RpcConstants.SYSTEM_PREFIX + "/oauth2/token";

    @GetMapping(PREFIX + "/check")
    CommonResult<OAuth2AccessTokenCheckRespDTO> checkAccessToken(@RequestParam("accessToken") String accessToken);
}
