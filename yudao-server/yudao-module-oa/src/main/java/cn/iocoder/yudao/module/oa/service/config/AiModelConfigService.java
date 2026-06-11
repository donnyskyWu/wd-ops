package cn.iocoder.yudao.module.oa.service.config;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.AiModelConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AiModelConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.AiModelConfigUpdateReq;

public interface AiModelConfigService {

    PageResult<AiModelConfigRespVO> list(String modelName, String status, Integer pageNo, Integer pageSize);

    Long create(AiModelConfigCreateReq req);

    void update(AiModelConfigUpdateReq req);

    void delete(Long id);

    cn.iocoder.yudao.module.oa.api.dto.config.AiModelStatsVO stats();

    boolean testConnection(Long id);

    void setDefault(Long id);
}
