package cn.iocoder.yudao.module.oa.service.config;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateAccountCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateAccountRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateAccountSyncRemoteReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateAccountSyncResultVO;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateAccountTestConnectionRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateAccountUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AochuangRemoteAccountRespVO;

import java.util.List;

public interface AoCreateAccountService {

    PageResult<AoCreateAccountRespVO> list(String accountName, String status, Integer pageNo, Integer pageSize);

    Long create(AoCreateAccountCreateReq req);

    void update(AoCreateAccountUpdateReq req);

    void delete(Long id);

    AoCreateAccountTestConnectionRespVO testConnection(Long id);

    List<AochuangRemoteAccountRespVO> listRemoteSubAccounts(String lastUpdateTime);

    AoCreateAccountSyncResultVO syncRemoteSubAccounts(AoCreateAccountSyncRemoteReq req);
}
