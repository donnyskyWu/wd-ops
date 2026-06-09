package cn.iocoder.yudao.module.oa.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.finance.AccountCostCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.finance.AccountCostUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.finance.AccountCostVO;

import java.time.LocalDate;

public interface AccountCostService {

    PageResult<AccountCostVO> list(Long accountId, String costType, LocalDate startDate, LocalDate endDate,
                                   Integer pageNum, Integer pageSize);

    Long create(AccountCostCreateReq req);

    void update(AccountCostUpdateReq req);

    void delete(Long id);
}
