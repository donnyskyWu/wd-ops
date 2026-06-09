package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M6FunnelS07IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M6-S-07: 漏斗列表≥2")
    void listFunnels() throws Exception {
        mockMvc.perform(get("/admin-api/oa/funnel/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageSize", "20"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("M6-S-07: 漏斗 data 非空")
    void funnelData() throws Exception {
        mockMvc.perform(get("/admin-api/oa/funnel/9801/data")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.steps.length()").value(greaterThan(0)));
    }
}
