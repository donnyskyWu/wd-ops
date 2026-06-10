package cn.iocoder.yudao.module.oa.service.plan;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanRespVO;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanTerminateReq;

public interface ContentPlanService {

    PageResult<ContentPlanRespVO> list(String planName, String status, Integer pageNo, Integer pageSize);

    ContentPlanRespVO get(Long id);

    Long create(ContentPlanCreateReq req);

    void start(Long id);

    void submitTerminate(Long id, ContentPlanTerminateReq req);

    void approveTerminate(Long id);

    void rejectTerminate(Long id);

    void delete(Long id);
}
