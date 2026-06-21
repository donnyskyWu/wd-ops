package cn.iocoder.yudao.module.oa.service.collect;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectLogRespVO;

public interface CollectLogService {

    PageResult<CollectLogRespVO> page(Long taskId, String status, String startDate, String endDate,
                                      Integer pageNo, Integer pageSize);
}
