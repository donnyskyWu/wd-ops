package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.service.collect.unified.WechatMpOfficialCredentialSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WechatMpOfficialCredentialSupportTest {

    @Test
    void certifiedWithAppCredentials() {
        AccountDO account = baseWechatAccount();
        account.setUsageStatus("CERTIFIED");
        account.setAppId("wx123");
        account.setAppSecretEncrypted("enc");
        assertTrue(WechatMpOfficialCredentialSupport.supportsOfficialApi(account));
    }

    @Test
    void renewedWithAppCredentials() {
        AccountDO account = baseWechatAccount();
        account.setUsageStatus("RENEWED");
        account.setAppId("wx123");
        account.setAppSecretEncrypted("enc");
        assertTrue(WechatMpOfficialCredentialSupport.supportsOfficialApi(account));
    }

    @Test
    void registeredWithoutOfficialApi() {
        AccountDO account = baseWechatAccount();
        account.setUsageStatus("REGISTERED");
        account.setAppId("wx123");
        account.setAppSecretEncrypted("enc");
        assertFalse(WechatMpOfficialCredentialSupport.supportsOfficialApi(account));
    }

    @Test
    void certifiedMissingSecret() {
        AccountDO account = baseWechatAccount();
        account.setUsageStatus("CERTIFIED");
        account.setAppId("wx123");
        assertFalse(WechatMpOfficialCredentialSupport.supportsOfficialApi(account));
    }

    @Test
    void nullUsageStatusWithoutOfficialApi() {
        AccountDO account = baseWechatAccount();
        account.setAppId("wx123");
        account.setAppSecretEncrypted("enc");
        assertFalse(WechatMpOfficialCredentialSupport.supportsOfficialApi(account));
    }

    private AccountDO baseWechatAccount() {
        AccountDO account = new AccountDO();
        account.setPlatformType("WECHAT_OFFICIAL");
        return account;
    }
}
