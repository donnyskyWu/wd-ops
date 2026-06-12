package cn.iocoder.yudao.module.oa.service.sop;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopTemplateCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopTemplateUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopTemplateVO;

public interface SopTemplateService {

    PageResult<SopTemplateVO> list(String templateName, String contentType, String platformType,
                                   Integer status, Integer pageNum, Integer pageSize);

    Long create(SopTemplateCreateReq req);

    void update(SopTemplateUpdateReq req);

    void delete(Long id);
}
