package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.module.oa.api.dto.account.FanGroupCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.account.FanGroupRespVO;
import cn.iocoder.yudao.module.oa.api.dto.account.FanGroupUpdateReq;

import java.util.List;

public interface PlatformAccountFanGroupService {

    List<FanGroupRespVO> listByAccount(Long accountId);

    Long create(FanGroupCreateReq req);

    void update(FanGroupUpdateReq req);

    void delete(Long id);
}
