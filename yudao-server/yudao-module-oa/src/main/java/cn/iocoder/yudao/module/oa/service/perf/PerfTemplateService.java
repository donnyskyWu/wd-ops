package cn.iocoder.yudao.module.oa.service.perf;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateActivateReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateItemsVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfTemplateDO;

public interface PerfTemplateService {

    PageResult<PerfTemplateVO> list(String position, Integer isActive, Integer pageNum, Integer pageSize);

    Long create(PerfTemplateCreateReq req);

    void update(PerfTemplateUpdateReq req);

    void activate(PerfTemplateActivateReq req);

    PerfTemplateItemsVO getItems(Long id);

    PerfTemplateDO findActiveByPosition(String position);
}
