package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.CollectConfigDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.config.CollectConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M8ColS06IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private CollectConfigMapper collectConfigMapper;

    @Test
    @DisplayName("M8-COL-S-06: V113 迁移 app_id 合并至 oa_account")
    void migrationMergedAppIdToAccount() {
        AccountDO account = accountMapper.selectById(9007L);
        assertNotNull(account);
        assertNotNull(account.getAppId());
        assertTrue(account.getAppId().startsWith("seed_app_"));

        long activePlatformRows = collectConfigMapper.selectCount(new LambdaQueryWrapper<CollectConfigDO>()
                .eq(CollectConfigDO::getTenantId, 1L)
                .eq(CollectConfigDO::getScope, "INTERNAL")
                .eq(CollectConfigDO::getAccountId, 9007L)
                .in(CollectConfigDO::getPlatformType,
                        "WECHAT_OFFICIAL", "DOUYIN", "KUAISHOU", "WECHAT_VIDEO", "SERVICE_ACCOUNT"));
        assertEquals(0L, activePlatformRows);
    }

    @Test
    @DisplayName("M8-COL-S-06: INTERNAL 平台类创建拒绝 1510")
    void platformInternalCreateRejected() throws Exception {
        mockMvc.perform(post("/admin-api/oa/config/internal-collect/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "configName": "M8-平台已退役",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9001,
                                  "collectFrequency": "DAILY",
                                  "collectMethod": "INTERNAL"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1510));
    }

    @Test
    @DisplayName("M8-COL-S-06: 奥创 aocreate 接口仍可用")
    void aoCreateStillWorks() throws Exception {
        mockMvc.perform(get("/admin-api/oa/config/internal-collect/aocreate")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));
    }
}
