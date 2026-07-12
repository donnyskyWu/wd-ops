package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountRespVO;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountUpdateReq;

/**
 * 微信公众号双写编排（mp_account SSOT + oa_account_ext，ADR-050 §C）。
 */
public interface PlatformAccountSyncService {

    PageResult<AccountRespVO> listWechatOfficial(String accountName, Long companyId, Long realnameId,
                                                   String status, Integer pageNo, Integer pageSize);

    AccountRespVO getWechatOfficial(Long mpAccountId);

    Long createWechatOfficial(AccountCreateReq req);

    void updateWechatOfficial(AccountUpdateReq req);
}
