package cn.iocoder.yudao.module.oa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S-R6：粉丝分析 stats 聚合端点 + export 端点 IT
 * spec: API-M1-运营管理 §4.4 / FR-M1-004 / AC-M1-004-1 / AC-M1-004-2
 */
@AutoConfigureMockMvc
class M1FollowerStatsIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * S-R6-B1+B4：stats 端点不依赖 list 分页，直接对所有匹配行聚合
     * 断言：4 个 KPI 字段为数字（非 null）、增长率 = 净增 / (maxFollower - 净增)
     */
    @Test
    @DisplayName("S-R6: stats 端点 — 30日窗口聚合 4 个 KPI 数字字段")
    void statsAggregatesFromSeedData() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/oa/follower-analysis/stats")
                        .param("startDate", "2026-05-10")
                        .param("endDate", "2026-06-08")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("totalFollowers").asLong()).isGreaterThan(0L);
        assertThat(data.get("newFollowers").asLong()).isGreaterThan(0L);
        assertThat(data.get("unfollowers").asLong()).isGreaterThan(0L);
        assertThat(data.get("netFollowers").asLong()).isGreaterThan(0L);
        // growthRate = 净增 / (maxFollower - 净增) * 100（已 ×100，前端不再乘）
        double net = data.get("netFollowers").asDouble();
        double max = data.get("totalFollowers").asDouble();
        double expectedRate = net / (max - net) * 100.0;
        assertThat(data.get("growthRate").asDouble())
                .isCloseTo(expectedRate, org.assertj.core.data.Offset.offset(0.01));
    }

    /**
     * S-R6-B1+B4：stats 端点空账号（ipGroupId 不可见）→ 全 0，不抛错
     */
    @Test
    @DisplayName("S-R6: stats 端点 — 不存在 accountId 全 0")
    void statsEmptyWhenNoAccount() throws Exception {
        mockMvc.perform(get("/admin-api/oa/follower-analysis/stats")
                        .param("accountId", "99999999")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalFollowers").value(0))
                .andExpect(jsonPath("$.data.newFollowers").value(0))
                .andExpect(jsonPath("$.data.unfollowers").value(0))
                .andExpect(jsonPath("$.data.netFollowers").value(0));
    }

    /**
     * S-R6-B2：dimension=WEEK 接受大小写不敏感 + 聚合按周
     */
    @Test
    @DisplayName("S-R6: stats dimension=WEEK 仍能聚合成功")
    void statsWithDimensionWeek() throws Exception {
        mockMvc.perform(get("/admin-api/oa/follower-analysis/stats")
                        .param("startDate", "2026-05-10")
                        .param("endDate", "2026-06-08")
                        .param("dimension", "week")  // 小写
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalFollowers").isNumber());
    }

    /**
     * S-R6-B2：trend 端点新增 dimension 参数向后兼容（不传 = 原始行为）
     */
    @Test
    @DisplayName("S-R6: trend 不传 dimension 仍能 200")
    void trendWithoutDimensionStillOk() throws Exception {
        mockMvc.perform(get("/admin-api/oa/follower-analysis/trend")
                        .param("startDate", "2026-05-10")
                        .param("endDate", "2026-06-08")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /**
     * S-R6-B3：export 端点返回 200 + text/csv + Content-Disposition 含 .csv
     * 注：mockMvc 不解析 Content-Disposition，只断言 status & content type prefix
     */
    @Test
    @DisplayName("S-R6: export 端点 — 返回 CSV 流")
    void exportReturnsCsv() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/oa/follower-analysis/export")
                        .param("startDate", "2026-05-10")
                        .param("endDate", "2026-06-08")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andReturn();
        String contentType = result.getResponse().getContentType();
        assertThat(contentType).startsWith("text/csv");
        String body = result.getResponse().getContentAsString();
        // 表头必须含 9 列
        assertThat(body).startsWith("\uFEFFstatDate,accountId,accountName,ipGroupName,followerCount,newFollower,unfollowCount,netGrowth,growthRate");
        // seed 应至少 30 行（10 天 × 3 账号）
        long rows = body.lines().count();
        assertThat(rows).isGreaterThan(30);
    }

    /**
     * S-R6-B3：export 0 匹配 → 仅 BOM + 表头，不报错
     */
    @Test
    @DisplayName("S-R6: export 不存在账号 — 返回仅表头的 CSV")
    void exportEmptyReturnsHeaderOnly() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/oa/follower-analysis/export")
                        .param("accountId", "99999999")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        // 1 行 = 表头
        assertThat(body.lines().count()).isEqualTo(1);
    }
}
