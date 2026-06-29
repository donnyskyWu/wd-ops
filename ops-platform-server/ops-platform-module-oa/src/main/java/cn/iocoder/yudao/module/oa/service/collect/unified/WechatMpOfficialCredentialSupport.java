package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;

import java.util.Set;

/**
 * 已认证公众号 Open API 凭证判定（ADR-047 Phase 2 · usage_status + AppID/AppSecret）。
 */
public final class WechatMpOfficialCredentialSupport {

    /** dict_wechat_usage_status：认证 / 续费（均已通过微信认证） */
    private static final Set<String> VERIFIED_USAGE_STATUS = Set.of("CERTIFIED", "RENEWED");

    private WechatMpOfficialCredentialSupport() {
    }

    public static boolean supportsOfficialApi(AccountDO account) {
        if (account == null || !"WECHAT_OFFICIAL".equals(account.getPlatformType())) {
            return false;
        }
        String usageStatus = account.getUsageStatus();
        return StrUtil.isNotBlank(usageStatus)
                && VERIFIED_USAGE_STATUS.contains(usageStatus)
                && StrUtil.isNotBlank(account.getAppId())
                && StrUtil.isNotBlank(account.getAppSecretEncrypted());
    }
}
