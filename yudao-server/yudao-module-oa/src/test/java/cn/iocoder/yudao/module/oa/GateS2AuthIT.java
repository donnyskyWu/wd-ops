package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class GateS2AuthIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String OPERATOR = "Bearer dev-token-oa-operator";
    private static final String TENANT_B = "Bearer dev-token-oa-tenantb";
    private static final String TENANT_1 = "1";
    private static final String TENANT_2 = "2";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("AUTH-001: 管理员 Token + tenant=1 → 200 且具备用户查询权限")
    void adminHasUserListPermission() throws Exception {
        mockMvc.perform(get("/admin-api/system/user/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("AUTH-002: 运营专员创建租户 → 403")
    void operatorCannotCreateTenant() throws Exception {
        mockMvc.perform(post("/admin-api/system/tenant/create")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "AUTH-无权限租户",
                                  "contactName": "测试",
                                  "contactPhone": "13800138999",
                                  "expireTime": "2027-12-31T23:59:59"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("AUTH-003: tenant-b Token 访问 tenant=1 业务数据 → 1504")
    void tenantBTokenCannotAccessTenant1Data() throws Exception {
        mockMvc.perform(get("/admin-api/oa/demo/items/1")
                        .header("Authorization", TENANT_B)
                        .header("X-Tenant-Id", TENANT_2))
                .andExpect(jsonPath("$.code").value(1504));
    }

    @Test
    @DisplayName("AUTH-004: 无 Authorization → 401")
    void missingTokenReturns401() throws Exception {
        mockMvc.perform(get("/admin-api/oa/hello")
                        .header("X-Tenant-Id", TENANT_1))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("AUTH-005: 运营专员 IP 组数据范围变窄")
    void operatorIpGroupDataScope() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1)
                        .param("accountName", "SEED-")
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(10));

        mockMvc.perform(get("/admin-api/oa/account/list")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT_1)
                        .param("accountName", "SEED-")
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(5));
    }
}
