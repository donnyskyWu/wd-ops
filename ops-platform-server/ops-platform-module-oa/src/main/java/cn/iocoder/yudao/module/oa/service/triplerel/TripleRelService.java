package cn.iocoder.yudao.module.oa.service.triplerel;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.triplerel.TripleRelCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.triplerel.TripleRelGraphRespVO;
import cn.iocoder.yudao.module.oa.api.dto.triplerel.TripleRelRespVO;
import cn.iocoder.yudao.module.oa.api.dto.triplerel.TripleRelStatisticsVO;

public interface TripleRelService {

    PageResult<TripleRelRespVO> list(String relationType, Integer status, Integer pageNo, Integer pageSize);

    TripleRelGraphRespVO graph(Long personalWechatId);

    Long create(TripleRelCreateReq req);

    void unbind(Long id);

    void rebind(Long id);

    TripleRelStatisticsVO statistics();
}
