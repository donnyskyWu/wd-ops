package cn.iocoder.yudao.module.oa.service.config;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.ThresholdConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.ThresholdConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.ThresholdConfigUpdateReq;

public interface ThresholdConfigService {

    PageResult<ThresholdConfigRespVO> list(String thresholdCategory, String metricName, String metricType,
                                           String status, Integer pageNo, Integer pageSize);

    Long create(ThresholdConfigCreateReq req);

    void update(ThresholdConfigUpdateReq req);

    void delete(Long id);
}
