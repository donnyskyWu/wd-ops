package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    // P-GATE-UNMOCK-R S-R2-B：账号粉丝详情端点（Q2：账号分析-粉丝详情不能点击）
    @Test
    @DisplayName("S-R2-B-01: 账号粉丝详情按账号ID返回趋势数据")
    void accountFollowers() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account-analysis/{id}/followers", 9001L)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].accountId").value(9001))
                .andExpect(jsonPath("$.data[0].accountName").exists())
                .andExpect(jsonPath("$.data[0].statDate").exists());
    }

    @Test
    @DisplayName("S-R2-B-02: 账号粉丝详情支持日期区间")
    void accountFollowersWithDateRange() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account-analysis/{id}/followers", 9001L)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-06-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("S-R2-B-03: 账号粉丝详情-不存在账号返回 1500 错误码（ENTITY_NOT_EXISTS）")
    void accountFollowersNotFound() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account-analysis/{id}/followers", 99999L)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1500));
    }

    // P-GATE-UNMOCK-R S-R2-B：账号作品详情端点（Q2：账号分析-作品详情不能点击）
    @Test
    @DisplayName("S-R2-B-04: 账号作品详情分页返回")
    void accountContents() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account-analysis/{id}/contents", 9001L)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("S-R2-B-05: 账号作品详情-不存在账号返回 1500 错误码（ENTITY_NOT_EXISTS）")
    void accountContentsNotFound() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account-analysis/{id}/contents", 99999L)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1500));
    }

    // P-GATE-UNMOCK-R S-R2-D：作品趋势聚合端点（Q4：作品分析无数据）
    @Test
    @DisplayName("S-R2-D-01: 作品趋势按日聚合返回阅读/点赞/评论/转发")
    void contentTrend() throws Exception {
        mockMvc.perform(get("/admin-api/oa/content-analysis/trend")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-06-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("S-R2-D-02: 作品趋势按 IP 组过滤")
    void contentTrendByIpGroup() throws Exception {
        mockMvc.perform(get("/admin-api/oa/content-analysis/trend")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("ipGroupId", "9001")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-06-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }
}
