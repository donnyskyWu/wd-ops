package cn.iocoder.yudao.framework.common.biz.infra.file;

import cn.iocoder.yudao.framework.common.biz.infra.file.dto.FileCreateReqDTO;
import cn.iocoder.yudao.framework.common.enums.RpcConstants;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Football infra-server file RPC (vendored subset, G-INF-01).
 * Aligns with {@code football.module.infra.api.file.FileApi}.
 */
@FeignClient(name = RpcConstants.INFRA_NAME, primary = false)
public interface FileApi {

    String PREFIX = RpcConstants.INFRA_PREFIX + "/file";

    @PostMapping(PREFIX + "/create")
    CommonResult<String> createFile(@Valid @RequestBody FileCreateReqDTO createReqDTO);

    @GetMapping(PREFIX + "/presigned-url")
    CommonResult<String> presignGetUrl(@NotEmpty @RequestParam("url") String url,
                                       @RequestParam(value = "expirationSeconds", required = false) Integer expirationSeconds);
}
