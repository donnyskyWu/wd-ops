package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.collect.DouyinFollowerDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.DouyinFollowerMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M4AccountDouyinFollowerIT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final long DOUYIN_ACCOUNT_ID = 9006L;
    private static final long WECHAT_ACCOUNT_ID = 9001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DouyinFollowerMapper douyinFollowerMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM oa_douyin_follower WHERE account_id = ?", DOUYIN_ACCOUNT_ID);
    }

    @Test
    @DisplayName("M4: 抖音粉丝列表分页查询")
    void pageDouyinFollowers() throws Exception {
        seedFollower("sec_uid_it_001", "IT抖音粉丝A");
        seedFollower("sec_uid_it_002", "IT抖音粉丝B");

        mockMvc.perform(get("/admin-api/oa/account/" + DOUYIN_ACCOUNT_ID + "/douyin-followers")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].nickname").exists())
                .andExpect(jsonPath("$.data.list[0].followerId").exists())
                .andExpect(jsonPath("$.data.list[0].followedAt").exists());
    }

    @Test
    @DisplayName("M4: 非抖音账号拒绝粉丝列表查询")
    void rejectNonDouyin() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account/" + WECHAT_ACCOUNT_ID + "/douyin-followers")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(1500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("抖音")));
    }

    private void seedFollower(String followerId, String nickname) {
        DouyinFollowerDO entity = new DouyinFollowerDO();
        entity.setTenantId(1L);
        entity.setAccountId(DOUYIN_ACCOUNT_ID);
        entity.setFollowerId(followerId);
        entity.setNickname(nickname);
        entity.setAvatar("https://example.com/avatar.png");
        entity.setFollowedAt(LocalDateTime.of(2026, 6, 1, 10, 0, 0));
        entity.setSyncedAt(LocalDateTime.of(2026, 6, 24, 8, 0, 0));
        douyinFollowerMapper.insert(entity);
    }
}
