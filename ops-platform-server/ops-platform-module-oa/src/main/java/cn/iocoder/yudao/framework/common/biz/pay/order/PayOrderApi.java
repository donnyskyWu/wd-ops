package cn.iocoder.yudao.framework.common.biz.pay.order;

import cn.iocoder.yudao.framework.common.biz.pay.order.dto.AllOrderRespDTO;
import cn.iocoder.yudao.framework.common.biz.pay.order.dto.OrderPageReqDTO;
import cn.iocoder.yudao.framework.common.enums.RpcConstants;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Football pay-server order RPC (vendored subset, G-PAY-01).
 * Aligns with {@code football.module.pay.api.order.PayOrderApi#getOrderPage}.
 */
@FeignClient(name = RpcConstants.PAY_NAME, primary = false)
public interface PayOrderApi {

    String PREFIX = RpcConstants.PAY_PREFIX + "/order";

    @PostMapping(PREFIX + "/page")
    CommonResult<PageResult<AllOrderRespDTO>> getOrderPage(@RequestBody OrderPageReqDTO pageReqDTO);
}
