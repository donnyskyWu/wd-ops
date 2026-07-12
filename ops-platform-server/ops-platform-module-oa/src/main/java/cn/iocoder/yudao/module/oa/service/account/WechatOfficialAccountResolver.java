package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.MpAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.OaAccountExtDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * 将 shenyu-mp.mp_account + wd.oa_account_ext 解析为 {@link AccountDO}，
 * 供读权限校验、粉丝列表、续费记录等与 {@link PlatformAccountSyncServiceImpl} 一致的 ID 语义。
 */
@Component
@RequiredArgsConstructor
public class WechatOfficialAccountResolver {

    private static final String PLATFORM_WECHAT_OFFICIAL = "WECHAT_OFFICIAL";

    private final MpAccountDataService mpAccountDataService;
    private final OaAccountExtDataService oaAccountExtDataService;

    public Optional<AccountDO> resolveReadableAccount(Long accountId, Long tenantId) {
        MpAccountDO mp = mpAccountDataService.selectById(accountId);
        if (mp == null || !Objects.equals(mp.getTenantId(), tenantId)) {
            return Optional.empty();
        }
        OaAccountExtDO ext = oaAccountExtDataService.findByMpAccountId(tenantId, accountId);
        AccountDO account = new AccountDO();
        account.setId(mp.getId());
        account.setTenantId(tenantId);
        account.setPlatformType(PLATFORM_WECHAT_OFFICIAL);
        account.setAccountName(mp.getName());
        account.setExternalAccountId(mp.getAccount());
        account.setAppId(mp.getAppId());
        account.setStatus(mp.getStatus() != null && mp.getStatus() == 0 ? "NORMAL" : "DISABLED");
        if (ext != null) {
            account.setCompanyId(ext.getCompanyId());
            account.setRealnameId(ext.getRealnameId());
            account.setIntermediaryId(ext.getIntermediaryId());
            account.setIpGroupId(ext.getIpGroupId());
            account.setPhoneId(ext.getPhoneId());
            account.setSimCardId(ext.getSimCardId());
            account.setTrademarkName(ext.getTrademarkName());
            account.setQualificationType(ext.getQualificationType());
            account.setUsageStatus(ext.getUsageStatus());
            account.setAdminUserId(ext.getAdminUserId());
        }
        return Optional.of(account);
    }

    public boolean isMpBackedAccount(Long accountId, Long tenantId) {
        MpAccountDO mp = mpAccountDataService.selectById(accountId);
        return mp != null && Objects.equals(mp.getTenantId(), tenantId);
    }
}
