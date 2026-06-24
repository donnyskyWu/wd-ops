package cn.iocoder.yudao.module.oa.service.collect;

import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorAccountBindRespVO;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorAccountBindSaveReq;

public interface CollectorAccountBindService {

    CollectorAccountBindRespVO getByOaAccountId(Long oaAccountId);

    Long saveOrUpdate(CollectorAccountBindSaveReq req);

    void updateConnStatus(Long oaAccountId, String connStatus, java.time.LocalDateTime healthCheckAt);
}
