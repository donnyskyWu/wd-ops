package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorRelVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorStatsVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorUpdateReq;

public interface OpsAnchorService {

    PageResult<OpsAnchorRelVO> list(Long opsUserId, Long anchorUserId, Integer pageNo, Integer pageSize);

    Long create(OpsAnchorCreateReq req);

    void update(OpsAnchorUpdateReq req);

    void delete(Long id);

    OpsAnchorStatsVO anchorStats(Long userId);
}
