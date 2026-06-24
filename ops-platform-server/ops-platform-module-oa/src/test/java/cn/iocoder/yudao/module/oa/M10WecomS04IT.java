package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * M10-WECOM-S-04: M1 微信分析 Tab 消费 oa_wework_daily_stats。
 */
@AutoConfigureMockMvc
class M10WecomS04IT extends OaITBase {

    private static final long TENANT_1 = 1L;
    private static final long WEWORK_ACCOUNT_ID = 9001L;
    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_1);
        TenantContextHolder.setUsername("it-m10-wecom-s04");
        cleanupStats();
        seedStats();
    }

    @AfterEach
    void tearDown() {
        cleanupStats();
        TenantContextHolder.clear();
    }

    private void cleanupStats() {
        jdbcTemplate.update(
                "DELETE FROM oa_wework_daily_stats WHERE tenant_id = ? AND wework_account_id = ?",
                TENANT_1, WEWORK_ACCOUNT_ID);
    }

    private void seedStats() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        jdbcTemplate.update("""
                INSERT INTO oa_wework_daily_stats
                (tenant_id, wework_account_id, stat_date, total_friends, today_friend_interactions,
                 today_messages_sent, synced_at, creator, updater)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 'it', 'it')
                """, TENANT_1, WEWORK_ACCOUNT_ID, today, 120, 15, 42);
        jdbcTemplate.update("""
                INSERT INTO oa_wework_daily_stats
                (tenant_id, wework_account_id, stat_date, total_friends, today_friend_interactions,
                 today_messages_sent, synced_at, creator, updater)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 'it', 'it')
                """, TENANT_1, WEWORK_ACCOUNT_ID, yesterday, 118, 12, 38);
    }

    @Test
    @DisplayName("M10-WECOM-S-04: GET /wechat-analysis/wework/list 返回日聚合")
    void listReturnsDailyStats() throws Exception {
        mockMvc.perform(get("/admin-api/oa/wechat-analysis/wework/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].accountId").value(WEWORK_ACCOUNT_ID))
                .andExpect(jsonPath("$.data.list[0].accountName").value("SEED-企微A"))
                .andExpect(jsonPath("$.data.list[0].totalFriends").value(120))
                .andExpect(jsonPath("$.data.list[0].todayFriendInteractions").value(15))
                .andExpect(jsonPath("$.data.list[0].todayMessagesSent").value(42));
    }

    @Test
    @DisplayName("M10-WECOM-S-04: GET /wechat-analysis/wework/detail 返回趋势")
    void detailReturnsTrend() throws Exception {
        mockMvc.perform(get("/admin-api/oa/wechat-analysis/wework/detail")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("accountId", String.valueOf(WEWORK_ACCOUNT_ID)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accountId").value(WEWORK_ACCOUNT_ID))
                .andExpect(jsonPath("$.data.dailyStats.length()").value(2))
                .andExpect(jsonPath("$.data.dailyStats[0].totalFriends").value(120))
                .andExpect(jsonPath("$.data.dailyStats[1].totalFriends").value(118));
    }
}
