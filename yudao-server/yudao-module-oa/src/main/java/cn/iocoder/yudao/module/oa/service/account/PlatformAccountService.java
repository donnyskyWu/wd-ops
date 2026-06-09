package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountReplaceReq;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountRespVO;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountUpdateReq;

public interface PlatformAccountService {

    PageResult<AccountRespVO> list(String platformType, String accountName, Long companyId,
                                   Long realnameId, String status, Integer pageNo, Integer pageSize);

    AccountRespVO get(Long id);

    Long create(AccountCreateReq req);

    void update(AccountUpdateReq req);

    void delete(Long id);

    void replace(Long id, AccountReplaceReq req);
}
