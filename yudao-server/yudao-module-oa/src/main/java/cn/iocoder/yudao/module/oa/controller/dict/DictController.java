package cn.iocoder.yudao.module.oa.controller.dict;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.dict.DictDataRespVO;
import cn.iocoder.yudao.module.oa.api.dto.dict.DictTypeRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.SysDictDataDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.SysDictTypeDO;
import cn.iocoder.yudao.module.oa.service.dict.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin-api/oa/dict")
@Validated
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @GetMapping("/data")
    public CommonResult<PageResult<DictDataRespVO>> data(@RequestParam(required = false) String type) {
        if (StrUtil.isBlank(type)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "字典 type 不能为空");
        }
        if (type.length() > 64) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "字典 type 长度超限");
        }
        if (!dictService.typeExists(type)) {
            throw new ServiceException(OaErrorCodes.DICT_TYPE_NOT_FOUND.getCode(), "字典 type 不存在：" + type);
        }
        List<DictDataRespVO> list = dictService.listByType(type).stream()
                .map(this::toDataVO)
                .collect(Collectors.toList());
        return CommonResult.success(new PageResult<>(list, (long) list.size()));
    }

    @GetMapping("/types")
    public CommonResult<PageResult<DictTypeRespVO>> types() {
        List<SysDictTypeDO> rows = dictService.listAllTypes();
        List<DictTypeRespVO> list = rows.stream()
                .map(t -> {
                    DictTypeRespVO vo = new DictTypeRespVO();
                    vo.setType(t.getType());
                    vo.setName(t.getName());
                    vo.setStatus(t.getStatus());
                    return vo;
                })
                .collect(Collectors.toList());
        return CommonResult.success(new PageResult<>(list, (long) list.size()));
    }

    private DictDataRespVO toDataVO(SysDictDataDO d) {
        DictDataRespVO vo = new DictDataRespVO();
        vo.setDictType(d.getDictType());
        vo.setLabel(d.getLabel());
        vo.setValue(d.getDictValue());
        vo.setSort(d.getSort());
        vo.setStatus(d.getStatus());
        return vo;
    }
}
