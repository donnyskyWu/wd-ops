package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WechatMpFollowerDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WechatMpFollowerMapper;
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
class M4AccountMpFollowerIT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final long WECHAT_ACCOUNT_ID = 9001L;
    private static final long DOUYIN_ACCOUNT_ID = 9006L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WechatMpFollowerMapper wechatMpFollowerMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM oa_wechat_mp_follower WHERE account_id = ?", WECHAT_ACCOUNT_ID);
    }

    @Test
    @DisplayName("M4: 公众号粉丝列表分页查询")
    void pageMpFollowers() throws Exception {
        seedFollower("oItFollower001", "IT粉丝A");
        seedFollower("oItFollower002", "IT粉丝B");

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

    private void seedFollower(String openid, String nickname) {
        WechatMpFollowerDO entity = new WechatMpFollowerDO();
        entity.setTenantId(1L);
        entity.setAccountId(WECHAT_ACCOUNT_ID);
        entity.setOpenid(openid);
        entity.setNickname(nickname);
        entity.setAvatar("https://example.com/avatar.png");
        entity.setSubscribedAt(LocalDateTime.of(2026, 6, 1, 10, 0, 0));
        entity.setSyncedAt(LocalDateTime.of(2026, 6, 24, 8, 0, 0));
        wechatMpFollowerMapper.insert(entity);
    }
}
