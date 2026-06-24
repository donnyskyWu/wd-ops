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
 * M10-AO-S-06 / M1 微信分析个微 Tab 消费 oa_personal_wechat_daily_stats。
 */
@AutoConfigureMockMvc
class M10PersonalWechatAnalysisIT extends OaITBase {

    private static final long TENANT_1 = 1L;
    private static final long PERSONAL_WECHAT_ID = 9001L;
    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_1);
        TenantContextHolder.setUsername("it-m10-personal-analysis");
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
                "DELETE FROM oa_personal_wechat_daily_stats WHERE tenant_id = ? AND personal_wechat_id = ?",
                TENANT_1, PERSONAL_WECHAT_ID);
    }

    private void seedStats() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        jdbcTemplate.update("""
                INSERT INTO oa_personal_wechat_daily_stats
                (tenant_id, personal_wechat_id, stat_date, total_friends, new_friends, deleted_friends,
                 messages_sent, messages_received, group_count, creator, updater)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'it', 'it')
                """, TENANT_1, PERSONAL_WECHAT_ID, today, 350, 5, 1, 28, 42, 12);
        jdbcTemplate.update("""
                INSERT INTO oa_personal_wechat_daily_stats
                (tenant_id, personal_wechat_id, stat_date, total_friends, new_friends, deleted_friends,
                 messages_sent, messages_received, group_count, creator, updater)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'it', 'it')
                """, TENANT_1, PERSONAL_WECHAT_ID, yesterday, 346, 3, 0, 22, 35, 11);
    }

    @Test
    @DisplayName("M10-AO-S-06: GET /wechat-analysis/personal/list 返回日聚合")
    void listReturnsDailyStats() throws Exception {
        mockMvc.perform(get("/admin-api/oa/wechat-analysis/personal/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("accountId", String.valueOf(PERSONAL_WECHAT_ID))
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].accountId").value(PERSONAL_WECHAT_ID))
                .andExpect(jsonPath("$.data.list[0].accountName").value("SEED-个微张三"))
                .andExpect(jsonPath("$.data.list[0].totalFriends").value(350))
                .andExpect(jsonPath("$.data.list[0].newFriends").value(5))
                .andExpect(jsonPath("$.data.list[0].messagesSent").value(28));
    }

    @Test
    @DisplayName("M10-AO-S-06: GET /wechat-analysis/personal/detail 返回趋势")
    void detailReturnsTrend() throws Exception {
        mockMvc.perform(get("/admin-api/oa/wechat-analysis/personal/detail")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("accountId", String.valueOf(PERSONAL_WECHAT_ID)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accountId").value(PERSONAL_WECHAT_ID))
                .andExpect(jsonPath("$.data.wechatId").value("seed_wx_zhangsan"))
                .andExpect(jsonPath("$.data.dailyStats.length()").value(2))
                .andExpect(jsonPath("$.data.dailyStats[0].totalFriends").value(350))
                .andExpect(jsonPath("$.data.dailyStats[0].messagesReceived").value(42))
                .andExpect(jsonPath("$.data.dailyStats[1].totalFriends").value(346));
    }
}
