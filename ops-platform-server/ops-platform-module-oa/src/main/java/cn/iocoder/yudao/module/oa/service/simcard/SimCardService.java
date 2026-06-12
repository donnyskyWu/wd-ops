package cn.iocoder.yudao.module.oa.service.simcard;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.simcard.LinkedAccountsRespVO;
import cn.iocoder.yudao.module.oa.api.dto.simcard.SimCardCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.simcard.SimCardRespVO;
import cn.iocoder.yudao.module.oa.api.dto.simcard.SimCardUpdateReq;

public interface SimCardService {

    PageResult<SimCardRespVO> list(String iccid, Long phoneId, String operator, String status,
                                   Integer pageNo, Integer pageSize);

    Long create(SimCardCreateReq req);

    void update(SimCardUpdateReq req);

    void delete(Long id);

    LinkedAccountsRespVO linkedAccounts(Long id, String platformType, String operator);
}
