package cn.iocoder.yudao.module.oa.service.analytics;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.analytics.DashboardCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.DashboardUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.DashboardVO;

public interface DashboardService {

    DashboardVO get(Long id);

    Long create(DashboardCreateReq req);

    PageResult<DashboardVO> listConfig(Integer pageNum, Integer pageSize);

    void updateConfig(Long id, String layout);

    void updateFull(DashboardUpdateReq req);
}
