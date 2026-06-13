package cn.iocoder.yudao.module.oa.service.system;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.system.ParamCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.ParamRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.ParamUpdateReq;

public interface ParamService {

    PageResult<ParamRespVO> list(String paramName, String paramKey, String category,
                                 Integer pageNo, Integer pageSize);

    Long create(ParamCreateReq req);

    void update(ParamUpdateReq req);

    void delete(Long id);

    String getString(Long tenantId, String paramKey, String defaultValue);

    boolean getBoolean(Long tenantId, String paramKey, boolean defaultValue);
}
