package cn.iocoder.yudao.module.oa.service.config;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.CollectConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.CollectConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.CollectConfigUpdateReq;

public interface CollectConfigService {

    PageResult<CollectConfigRespVO> list(String scope, String configName, String subType, String platformType,
                                         String status, Integer pageNo, Integer pageSize);

    Long create(String scope, CollectConfigCreateReq req);

    void update(String scope, CollectConfigUpdateReq req);

    void delete(String scope, Long id);

    void toggleStatus(String scope, Long id, String status);

    boolean testConnection(String scope, Long id);

    cn.iocoder.yudao.module.oa.api.dto.config.ImportResultVO importExternalAccounts(String csvContent);
}
