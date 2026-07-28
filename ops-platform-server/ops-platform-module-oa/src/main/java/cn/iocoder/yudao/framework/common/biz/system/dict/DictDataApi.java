package cn.iocoder.yudao.framework.common.biz.system.dict;

import cn.iocoder.yudao.framework.common.biz.system.dict.dto.DictDataRespDTO;
import cn.iocoder.yudao.framework.common.enums.RpcConstants;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

/**
 * Football system-server dict RPC (vendored subset, G-DICT-01).
 * Aligns with {@code football.module.system.api.dict.DictDataApi} + {@code DictDataCommonApi}.
 */
@FeignClient(name = RpcConstants.SYSTEM_NAME, primary = false)
public interface DictDataApi {

    String PREFIX = RpcConstants.SYSTEM_PREFIX + "/dict-data";

    @GetMapping(PREFIX + "/list")
    CommonResult<List<DictDataRespDTO>> getDictDataList(@RequestParam("dictType") String dictType);

    @GetMapping(PREFIX + "/valid")
    CommonResult<Boolean> validateDictDataList(@RequestParam("dictType") String dictType,
                                               @RequestParam("values") Collection<String> values);
}
