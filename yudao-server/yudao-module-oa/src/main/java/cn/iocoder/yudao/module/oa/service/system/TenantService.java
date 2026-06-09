package cn.iocoder.yudao.module.oa.service.system;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.system.TenantCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.TenantRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.TenantUpdateReq;

public interface TenantService {

    PageResult<TenantRespVO> list(String tenantName, String contactName, String status,
                                  Integer pageNo, Integer pageSize);

    Long create(TenantCreateReq req);

    void update(TenantUpdateReq req);

    void delete(Long id);
}
