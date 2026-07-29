package cn.iocoder.yudao.framework.common.biz.system.logger;

import cn.iocoder.yudao.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import cn.iocoder.yudao.framework.common.enums.RpcConstants;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Football system-server operate-log RPC (vendored from yudao-common, AL-05).
 */
@FeignClient(contextId = "operateLogCommonApi", name = RpcConstants.SYSTEM_NAME, primary = false)
public interface OperateLogCommonApi {

    String PREFIX = RpcConstants.SYSTEM_PREFIX + "/operate-log";

    @PostMapping(PREFIX + "/create")
    CommonResult<Boolean> createOperateLog(@Valid @RequestBody OperateLogCreateReqDTO createReqDTO);

    @Async
    default void createOperateLogAsync(OperateLogCreateReqDTO createReqDTO) {
        createOperateLog(createReqDTO).checkError();
    }
}
