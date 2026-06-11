package cn.iocoder.yudao.module.oa.service.system;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.system.DictAdminRowVO;
import cn.iocoder.yudao.module.oa.api.dto.system.DictCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.DictTypeDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.system.DictUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.dict.DictTypeRespVO;

import java.util.List;

public interface SystemDictService {

    List<DictTypeRespVO> typeList();

    DictTypeDetailVO getByType(String type);

    PageResult<DictAdminRowVO> adminList(String dictName, String dictType, String status,
                                         Integer pageNo, Integer pageSize);

    Long create(DictCreateReq req);

    void update(DictUpdateReq req);

    void deleteData(Long id);
}
