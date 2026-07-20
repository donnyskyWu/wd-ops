package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M4AccountMpFollowerIT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final long WECHAT_ACCOUNT_ID = 9001L;
    private static final long DOUYIN_ACCOUNT_ID = 9006L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        ensureMpUserTable();
        jdbcTemplate.update("DELETE FROM mp_user WHERE account_id = ?", WECHAT_ACCOUNT_ID);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM mp_user WHERE account_id = ?", WECHAT_ACCOUNT_ID);
    }

    @Test
    @DisplayName("M4: 公众号粉丝列表分页查询（Football mp_user）")
    void pageMpFollowers() throws Exception {
        seedMpUser("oItFollower001", "IT粉丝A");
        seedMpUser("oItFollower002", "IT粉丝B");

        mockMvc.perform(get("/admin-api/oa/account/" + WECHAT_ACCOUNT_ID + "/mp-followers")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].nickname").exists())
                .andExpect(jsonPath("$.data.list[0].openid").exists())
                .andExpect(jsonPath("$.data.list[0].avatar").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.data.list[0].subscribedAt").exists());
    }

    @Test
    @DisplayName("M4: 非公众号账号拒绝粉丝列表查询")
    void rejectNonWechatOfficial() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account/" + DOUYIN_ACCOUNT_ID + "/mp-followers")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(1500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("公众号")));
    }

    private void ensureMpUserTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS mp_user (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    openid VARCHAR(100) NOT NULL,
                    union_id VARCHAR(128),
                    subscribe_status TINYINT NOT NULL DEFAULT 1,
                    subscribe_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    nickname VARCHAR(64),
                    head_image_url VARCHAR(1024),
                    account_id BIGINT NOT NULL,
                    app_id VARCHAR(128) NOT NULL DEFAULT '',
                    tenant_id BIGINT NOT NULL DEFAULT 1,
                    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    deleted TINYINT NOT NULL DEFAULT 0
                )
                """);
    }

    private void seedMpUser(String openid, String nickname) {
        jdbcTemplate.update("""
                INSERT INTO mp_user (openid, nickname, head_image_url, account_id, app_id, tenant_id,
                                     subscribe_status, subscribe_time, update_time, deleted)
                VALUES (?, ?, ?, ?, 'wx-test', 1, 1, TIMESTAMP '2026-06-01 10:00:00',
                        TIMESTAMP '2026-06-24 08:00:00', 0)
                """, openid, nickname, "https://example.com/avatar.png", WECHAT_ACCOUNT_ID);
    }
}
