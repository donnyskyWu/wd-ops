package cn.iocoder.yudao.module.oa.service.bridging;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.bridging.PrivateDomainBridgeCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.bridging.PrivateDomainBridgeRejectReq;
import cn.iocoder.yudao.module.oa.api.dto.bridging.PrivateDomainBridgeRespVO;

public interface PrivateDomainBridgeService {

    PageResult<PrivateDomainBridgeRespVO> page(String reviewStatus, String sourceType, String targetType,
                                               String matchMethod, Integer pageNo, Integer pageSize);

    PrivateDomainBridgeRespVO get(Long id);

    Long create(PrivateDomainBridgeCreateReq req);

    void confirm(Long id);

    void reject(Long id, PrivateDomainBridgeRejectReq req);

    boolean existsPair(Long tenantId, String sourceType, Long sourceId, String targetType, Long targetId);

    Long createBridge(Long tenantId, String sourceType, Long sourceId, String targetType, Long targetId,
                      String matchMethod, java.math.BigDecimal confidence, String evidenceJson, String reviewStatus);
}
