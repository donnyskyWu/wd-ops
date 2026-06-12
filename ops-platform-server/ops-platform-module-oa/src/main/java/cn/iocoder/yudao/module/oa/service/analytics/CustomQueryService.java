package cn.iocoder.yudao.module.oa.service.analytics;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.analytics.CustomQueryCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.CustomQueryPreviewReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.CustomQueryUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.CustomQueryDO;
import cn.iocoder.yudao.module.oa.service.support.DashboardSqlParamBinder;

import java.util.Map;

public interface CustomQueryService {

    PageResult<CustomQueryDO> list(String status, Integer pageNum, Integer pageSize);

    Long create(CustomQueryCreateReq req);

    void update(CustomQueryUpdateReq req);

    Map<String, Object> preview(CustomQueryPreviewReq req);

    Map<String, Object> execute(Long id);

    Map<String, Object> execute(Long id, DashboardSqlParamBinder.BindParams bindParams);

    Map<String, Object> execute(Long id, DashboardSqlParamBinder.BindParams bindParams,
                                Map<String, Object> widgetDef);

    void publish(Long id);
}
