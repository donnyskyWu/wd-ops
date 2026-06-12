package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class DashboardRoutingIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Dashboard: /create 不会被 /{id} 拦截")
    void createNotIntercepted() throws Exception {
        mockMvc.perform(post("/admin-api/oa/dashboard/create")
                        .header("Authorization", ADMIN).header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dashboardName":"IT-测试","dashboardType":"OPS","layout":"{}"}
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("Dashboard: config list")
    void configList() throws Exception {
        mockMvc.perform(get("/admin-api/oa/dashboard-config/list")
                        .header("Authorization", ADMIN).header("X-Tenant-Id", TENANT)
                        .param("pageNum", "1").param("pageSize", "20"))
                .andExpect(jsonPath("$.code").value(0));
    }
}
