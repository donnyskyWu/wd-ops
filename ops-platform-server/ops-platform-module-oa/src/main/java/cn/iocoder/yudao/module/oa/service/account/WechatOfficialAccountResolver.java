package cn.iocoder.yudao.module.oa.service.account;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.MpAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.OaAccountExtDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
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

    private final AccountMapper accountMapper;
    private final MpAccountDataService mpAccountDataService;
    private final OaAccountExtDataService oaAccountExtDataService;

    /**
     * 解析租户内平台账号：优先 oa_account，公众号 SSOT 回退 mp_account + oa_account_ext。
     */
    public AccountDO requireTenantAccount(Long accountId, Long tenantId) {
        AccountDO account = accountMapper.selectById(accountId);
        if (account != null && Objects.equals(account.getTenantId(), tenantId)) {
            return account;
        }
        return resolveReadableAccount(accountId, tenantId)
                .orElseThrow(() -> new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "账号不存在"));
    }

    public void assertTenantAccount(Long accountId, Long tenantId) {
        requireTenantAccount(accountId, tenantId);
    }

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
            if (StrUtil.isNotBlank(ext.getCookieEncrypted())) {
                account.setCookieEncrypted(ext.getCookieEncrypted());
            }
            if (StrUtil.isNotBlank(ext.getMpTokenEncrypted())) {
                account.setMpTokenEncrypted(ext.getMpTokenEncrypted());
            }
        }
        if (StrUtil.isNotBlank(mp.getAppSecret())) {
            account.setAppSecretEncrypted(mp.getAppSecret());
        }
        return Optional.of(account);
    }

    public boolean isMpBackedAccount(Long accountId, Long tenantId) {
        MpAccountDO mp = mpAccountDataService.selectById(accountId);
        return mp != null && Objects.equals(mp.getTenantId(), tenantId);
    }

    /**
     * 解析 Football mp_account.id，供 mp_user 粉丝列表等跨库读。
     */
    public Optional<Long> resolveMpAccountId(Long accountId, Long tenantId) {
        MpAccountDO mp = mpAccountDataService.selectById(accountId);
        if (mp != null && Objects.equals(mp.getTenantId(), tenantId)) {
            return Optional.of(mp.getId());
        }
        AccountDO oa = accountMapper.selectById(accountId);
        if (oa == null || !Objects.equals(oa.getTenantId(), tenantId)) {
            return Optional.empty();
        }
        if (!PLATFORM_WECHAT_OFFICIAL.equals(oa.getPlatformType())) {
            return Optional.empty();
        }
        if (StrUtil.isNotBlank(oa.getAppId())) {
            MpAccountDO byAppId = mpAccountDataService.selectByAppId(tenantId, oa.getAppId());
            if (byAppId != null) {
                return Optional.of(byAppId.getId());
            }
        }
        return Optional.of(accountId);
    }
}
