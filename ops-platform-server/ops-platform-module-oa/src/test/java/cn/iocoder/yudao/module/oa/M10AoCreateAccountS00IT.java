package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
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
class M10AoCreateAccountS00IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final String TENANT_B = "2";
    private static final String TENANT_B_AUTH = "Bearer dev-token-oa-tenantb";

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void seedAoCreateApi() throws Exception {
        mockMvc.perform(post("/admin-api/oa/config/internal-collect/aocreate")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "apiUrl": "https://ao.stub.local",
                                  "appId": "stub-app-id",
                                  "appSecret": "stub-secret",
                                  "token": "stub-token"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M10-AO-S-00: 奥创账号 CRUD + 列表")
    void aocreateAccountCrud() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/config/internal-collect/aocreate/accounts/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountName": "华东客服组",
                                  "aochuangAccountId": "acct-001",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long id = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get("/admin-api/oa/config/internal-collect/aocreate/accounts/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("accountName", "华东"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].aochuangAccountId").value("acct-001"));

        mockMvc.perform(put("/admin-api/oa/config/internal-collect/aocreate/accounts/update")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "id": %d,
                                  "accountName": "华东客服组V2",
                                  "aochuangAccountId": "acct-001",
                                  "status": "ENABLED"
                                }
                                """, id)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(delete("/admin-api/oa/config/internal-collect/aocreate/accounts/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(id)))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M10-AO-S-00: 重复奥创账号 ID → 2021")
    void duplicateAochuangAccountIdFails() throws Exception {
        mockMvc.perform(post("/admin-api/oa/config/internal-collect/aocreate/accounts/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountName": "账号A",
                                  "aochuangAccountId": "dup-acct",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/admin-api/oa/config/internal-collect/aocreate/accounts/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountName": "账号B",
                                  "aochuangAccountId": "dup-acct",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(2021));
    }

    @Test
    @DisplayName("M10-AO-S-00: 测试连接 stub 成功")
    void testConnectionStubSuccess() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/config/internal-collect/aocreate/accounts/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountName": "测试账号",
                                  "aochuangAccountId": "acct-test",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long id = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(post("/admin-api/oa/config/internal-collect/aocreate/accounts/" + id + "/test-connection")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.deviceCount").value(1))
                .andExpect(jsonPath("$.data.connStatus").value("OK"));
    }

    @Test
    @DisplayName("M10-AO-S-00: 跨租户访问 → 1504")
    void crossTenantForbidden() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/config/internal-collect/aocreate/accounts/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountName": "租户1账号",
                                  "aochuangAccountId": "tenant1-acct",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long id = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get("/admin-api/oa/config/internal-collect/aocreate/accounts/list")
                        .header("Authorization", TENANT_B_AUTH)
                        .header("X-Tenant-Id", TENANT_B)
                        .param("accountName", "租户1"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(post("/admin-api/oa/config/internal-collect/aocreate/accounts/" + id + "/test-connection")
                        .header("Authorization", TENANT_B_AUTH)
                        .header("X-Tenant-Id", TENANT_B))
                .andExpect(jsonPath("$.code").value(1504));
    }

    @Test
    @DisplayName("M10-AO-S-00: 非法状态字典 → 1503")
    void invalidStatusFails() throws Exception {
        mockMvc.perform(post("/admin-api/oa/config/internal-collect/aocreate/accounts/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountName": "非法状态",
                                  "aochuangAccountId": "bad-status",
                                  "status": "INVALID_STATUS"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1503));
    }
}
