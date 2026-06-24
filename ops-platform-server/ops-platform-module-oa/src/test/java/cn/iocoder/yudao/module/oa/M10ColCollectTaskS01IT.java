package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.service.collect.unified.UnifiedCollectorAdapter;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class M10ColCollectTaskS01IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final String TENANT_B = "2";
    private static final String TENANT_B_AUTH = "Bearer dev-token-oa-tenantb";
    private static final long SEED_ACCOUNT_ID = 9001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UnifiedCollectorAdapter unifiedCollectorAdapter;

    @Autowired
    private cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper accountMapper;

    @Autowired
    private AesUtil aesUtil;

    @Test
    @DisplayName("M10-COL-S-01: 采集任务 CRUD + 分页")
    void collectTaskCrud() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "公众号每日采集",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9001,
                                  "method": "INTERNAL",
                                  "source": "WECHAT_MP_API",
                                  "frequency": "DAILY",
                                  "cron": "0 0 2 * * ?",
                                  "apiConfig": "{\\"apiKey\\":\\"secret-key\\"}",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long id = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get("/admin-api/oa/collect/task/page")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("name", "公众号"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].name").value("公众号每日采集"))
                .andExpect(jsonPath("$.data.list[0].apiConfig").value("******"));

        mockMvc.perform(get("/admin-api/oa/collect/task/" + id)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accountName").value("SEED-公众号A1"));

        mockMvc.perform(put("/admin-api/oa/collect/task/update")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "id": %d,
                                  "name": "公众号每日采集V2",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9001,
                                  "method": "INTERNAL",
                                  "source": "WECHAT_MP_API",
                                  "frequency": "DAILY",
                                  "cron": "0 0 3 * * ?",
                                  "status": "PENDING"
                                }
                                """, id)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(delete("/admin-api/oa/collect/task/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(id)))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M10-COL-S-01: cron 非法 → 1400")
    void invalidCronFails() throws Exception {
        mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "非法cron任务",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9001,
                                  "method": "INTERNAL",
                                  "source": "WECHAT_MP_API",
                                  "frequency": "DAILY",
                                  "cron": "invalid",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1400));
    }

    @Test
    @DisplayName("M10-COL-S-01: 字典非法 → 1503")
    void invalidDictFails() throws Exception {
        mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "字典非法",
                                  "platformType": "INVALID_PLATFORM",
                                  "accountId": 9001,
                                  "method": "INTERNAL",
                                  "source": "WECHAT_MP_API",
                                  "frequency": "DAILY",
                                  "cron": "0 0 2 * * ?",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1503));
    }

    @Test
    @DisplayName("M10-COL-S-01: 账号不存在 → 1500")
    void missingAccountFails() throws Exception {
        mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "无账号",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 99999999,
                                  "method": "INTERNAL",
                                  "source": "WECHAT_MP_API",
                                  "frequency": "DAILY",
                                  "cron": "0 0 2 * * ?",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1500));
    }

    @Test
    @DisplayName("M10-COL-S-01: 跨租户 → 1504")
    void crossTenantForbidden() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "租户隔离任务",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9001,
                                  "method": "INTERNAL",
                                  "source": "WECHAT_MP_API",
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

    @Test
    @DisplayName("M10-COL-S-01: 立即执行写入日志")
    void runCreatesLog() throws Exception {
        TenantContextHolder.setTenantId(Long.parseLong(TENANT));
        TenantContextHolder.setUsername("it-m10-col-s01");
        try {
            AccountDO account = accountMapper.selectById(SEED_ACCOUNT_ID);
            account.setCookieEncrypted(aesUtil.encrypt("bizuin=123; data_bizuin=123"));
            account.setMpTokenEncrypted(aesUtil.encrypt("1234567890"));
            accountMapper.updateById(account);
            unifiedCollectorAdapter.bindAccount(SEED_ACCOUNT_ID);

            MvcResult createResult = mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "立即执行任务",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9001,
                                  "method": "INTERNAL",
                                  "source": "WECHAT_MP_API",
                                  "frequency": "DAILY",
                                  "cron": "0 0 2 * * ?",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long id = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(post("/admin-api/oa/collect/task/" + id + "/run")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/collect/log/page")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("taskId", String.valueOf(id)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].status").value("SUCCESS"));
        } finally {
            TenantContextHolder.clear();
        }
    }
}
