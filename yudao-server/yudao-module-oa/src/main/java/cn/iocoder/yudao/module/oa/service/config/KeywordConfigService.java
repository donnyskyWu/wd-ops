package cn.iocoder.yudao.module.oa.service.config;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.KeywordConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.KeywordConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.KeywordConfigUpdateReq;

public interface KeywordConfigService {

    PageResult<KeywordConfigRespVO> list(String platform, String keyword, String status,
                                         Integer pageNo, Integer pageSize);

    Long create(KeywordConfigCreateReq req);

    void update(KeywordConfigUpdateReq req);

    void delete(Long id);
}
