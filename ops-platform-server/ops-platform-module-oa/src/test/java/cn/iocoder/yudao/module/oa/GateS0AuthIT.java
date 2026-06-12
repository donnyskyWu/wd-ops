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
class GateS0AuthIT extends OaITBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("AUTH-004: 无 Authorization → 401")
    void helloWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/admin-api/oa/hello")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("HelloWorld + dev-token-oa-admin → 200")
    void helloWithDevTokenReturns200() throws Exception {
        mockMvc.perform(get("/admin-api/oa/hello")
                        .header("Authorization", "Bearer dev-token-oa-admin")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(1001))
                .andExpect(jsonPath("$.data.tenantId").value(1));
    }

    @Test
    @DisplayName("AUTH-003: Token 与 X-Tenant-Id 不一致 → 1504")
    void tokenTenantMismatchReturns1504() throws Exception {
        mockMvc.perform(get("/admin-api/oa/hello")
                        .header("Authorization", "Bearer dev-token-oa-admin")
                        .header("X-Tenant-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1504));
    }

    @Test
    @DisplayName("@InDict 非法值 → 1503")
    void invalidDictValueReturns1503() throws Exception {
        mockMvc.perform(post("/admin-api/oa/demo/validate-dict")
                        .header("Authorization", "Bearer dev-token-oa-admin")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platformType\":\"INVALID_VALUE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1503));
    }

    @Test
    @DisplayName("跨租户访问业务数据 → 1504")
    void crossTenantDataAccessReturns1504() throws Exception {
        mockMvc.perform(get("/admin-api/oa/demo/items/2")
                        .header("Authorization", "Bearer dev-token-oa-admin")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1504));
    }
}
