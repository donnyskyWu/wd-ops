package cn.iocoder.yudao.module.oa.service.collect;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectTaskCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectTaskRespVO;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectTaskUpdateReq;

public interface CollectTaskService {

    PageResult<CollectTaskRespVO> page(String name, String platformType, String method,
                                       String frequency, String status,
                                       Integer pageNo, Integer pageSize);

    CollectTaskRespVO get(Long id);

    Long create(CollectTaskCreateReq req);

    void update(CollectTaskUpdateReq req);

    void delete(Long id);

    void run(Long id);

    void updateStatus(Long id, String status);
}
