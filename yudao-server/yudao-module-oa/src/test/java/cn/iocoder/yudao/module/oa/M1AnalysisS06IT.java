package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M1AnalysisS06IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M1-S-06: 账号分析列表按平台筛选")
    void accountAnalysisList() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account-analysis/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("platform", "WECHAT_OFFICIAL")
                        .param("ipGroupId", "9001"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    @DisplayName("M1-S-07: 粉丝趋势")
    void followerTrend() throws Exception {
        mockMvc.perform(get("/admin-api/oa/follower-analysis/trend")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("accountId", "9001")
                        .param("startDate", "2026-05-10")
                        .param("endDate", "2026-06-08"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(30));
    }

    @Test
    @DisplayName("M1-S-07: 作品列表含爆款标识")
    void contentAnalysisList() throws Exception {
        mockMvc.perform(get("/admin-api/oa/content-analysis/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("ipGroupId", "9001")
                        .param("isHit", "true"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.list[0].isHit").value(true));
    }
}
