package cn.iocoder.yudao.module.oa.service.config;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.AiPromptConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AiPromptConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.AiPromptConfigUpdateReq;

public interface AiPromptConfigService {

    PageResult<AiPromptConfigRespVO> list(String templateName, String scene, String status,
                                          Integer pageNo, Integer pageSize);

    Long create(AiPromptConfigCreateReq req);

    void update(AiPromptConfigUpdateReq req);

    void delete(Long id);
}
