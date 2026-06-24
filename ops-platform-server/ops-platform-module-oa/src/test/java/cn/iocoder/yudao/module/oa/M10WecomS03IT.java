package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * M10-WECOM-S-03: 企微采集任务 CRUD 校验（account_id → oa_wework_account.id）。
 */
@AutoConfigureMockMvc
class M10WecomS03IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final String TENANT_B = "2";
    private static final String TENANT_B_AUTH = "Bearer dev-token-oa-tenantb";
    private static final long WEWORK_ACCOUNT_ID = 9001L;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M10-WECOM-S-03: 创建企微任务校验 wework account + 默认 dataType")
    void createWeworkTaskValidatesAccount() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "企微日统计采集",
                                  "platformType": "WEWORK",
                                  "accountId": 9001,
                                  "method": "INTERNAL",
                                  "source": "WECOM_API",
                                  "frequency": "DAILY",
                                  "cron": "0 0 2 * * ?",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long id = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get("/admin-api/oa/collect/task/" + id)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accountName").value("SEED-企微A"))
                .andExpect(jsonPath("$.data.dataType").value("WECOM_DAILY_STATS"))
                .andExpect(jsonPath("$.data.source").value("WECOM_API"))
                .andExpect(jsonPath("$.data.platformType").value("WEWORK"));
    }

    @Test
    @DisplayName("M10-WECOM-S-03: 企微账号不存在 → 1500")
    void missingWeworkAccountFails() throws Exception {
        mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "无效企微账号",
                                  "platformType": "WEWORK",
                                  "accountId": 99999999,
                                  "method": "INTERNAL",
                                  "source": "WECOM_API",
                                  "frequency": "DAILY",
                                  "cron": "0 0 2 * * ?",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1500));
    }

    @Test
    @DisplayName("M10-WECOM-S-03: 平台账号 ID 不能冒充企微账号 → 1500")
    void platformAccountIdNotAcceptedForWeworkTask() throws Exception {
        mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "误用平台账号",
                                  "platformType": "WEWORK",
                                  "accountId": 9010,
                                  "method": "INTERNAL",
                                  "source": "WECOM_API",
                                  "frequency": "DAILY",
                                  "cron": "0 0 2 * * ?",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1500));
    }

    @Test
    @DisplayName("M10-WECOM-S-03: 跨租户 → 1504")
    void crossTenantForbidden() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "租户隔离企微任务",
                                  "platformType": "WEWORK",
                                  "accountId": 9001,
                                  "method": "INTERNAL",
                                  "source": "WECOM_API",
                                  "frequency": "DAILY",
                                  "cron": "0 0 2 * * ?",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long id = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get("/admin-api/oa/collect/task/" + id)
                        .header("Authorization", TENANT_B_AUTH)
                        .header("X-Tenant-Id", TENANT_B))
                .andExpect(jsonPath("$.code").value(1504));
    }
}
