package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.AccountStatusLogDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.DouyinFollowerDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.DouyinVideoDO;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.AccountStatusLogMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.DouyinFollowerMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.DouyinVideoMapper;
import cn.iocoder.yudao.module.oa.service.collect.display.CollectedDataQueryService;
import cn.iocoder.yudao.module.oa.service.operations.AccountAnalysisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M10 collected tables should surface in M1/M4 analysis endpoints.
 */
@AutoConfigureMockMvc
class M10CollectedDataDisplayIT extends OaITBase {

    private static final long TENANT_1 = 1L;
    private static final long DOUYIN_ACCOUNT_ID = 9006L;
    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final String VIDEO_ID = "it_display_douyin_video_001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountStatusLogMapper accountStatusLogMapper;

    @Autowired
    private DouyinVideoMapper douyinVideoMapper;

    @Autowired
    private DouyinFollowerMapper douyinFollowerMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CollectedDataQueryService collectedDataQueryService;

    @Autowired
    private AccountAnalysisService accountAnalysisService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_1);
        TenantContextHolder.setUsername("it-m10-display");
        cleanup();
        seedCollectedData();
    }

    @AfterEach
    void tearDown() {
        cleanup();
        TenantContextHolder.clear();
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM oa_douyin_follower WHERE account_id = ? AND follower_id LIKE 'it_display_%'",
                DOUYIN_ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM oa_douyin_video WHERE account_id = ? AND video_id = ?",
                DOUYIN_ACCOUNT_ID, VIDEO_ID);
        jdbcTemplate.update("DELETE FROM oa_account_status_log WHERE account_id = ? AND stat_date = CURRENT_DATE",
                DOUYIN_ACCOUNT_ID);
    }

    private void seedCollectedData() {
        AccountStatusLogDO statusLog = new AccountStatusLogDO();
        statusLog.setTenantId(TENANT_1);
        statusLog.setAccountId(DOUYIN_ACCOUNT_ID);
        statusLog.setStatDate(LocalDate.now());
        statusLog.setStatus("NORMAL");
        statusLog.setFollowerCount(12_345L);
        accountStatusLogMapper.insert(statusLog);

        DouyinVideoDO video = new DouyinVideoDO();
        video.setTenantId(TENANT_1);
        video.setAccountId(DOUYIN_ACCOUNT_ID);
        video.setVideoId(VIDEO_ID);
        video.setTitle("IT-DISPLAY-DOUYIN-WORK");
        video.setPublishedAt(LocalDateTime.now().minusDays(1));
        video.setPlayCount(56_000);
        video.setLikeCount(1200);
        video.setCommentCount(88);
        video.setShareCount(66);
        video.setSyncedAt(LocalDateTime.now());
        douyinVideoMapper.insert(video);

        DouyinFollowerDO follower = new DouyinFollowerDO();
        follower.setTenantId(TENANT_1);
        follower.setAccountId(DOUYIN_ACCOUNT_ID);
        follower.setFollowerId("it_display_follower_001");
        follower.setNickname("展示粉丝A");
        follower.setFollowedAt(LocalDateTime.now().minusDays(2));
        follower.setSyncedAt(LocalDateTime.now());
        douyinFollowerMapper.insert(follower);
    }

    @Test
    @DisplayName("M10: CollectedDataQueryService 直接读取采集表")
    void collectedDataQueryServiceReadsM10Tables() {
        org.junit.jupiter.api.Assertions.assertEquals(12345L,
                collectedDataQueryService.latestFollowerCount(TENANT_1, DOUYIN_ACCOUNT_ID));
        org.junit.jupiter.api.Assertions.assertEquals(1,
                collectedDataQueryService.workCount(TENANT_1, DOUYIN_ACCOUNT_ID, "DOUYIN"));
        org.junit.jupiter.api.Assertions.assertEquals(1L,
                collectedDataQueryService.pageCollectedContents(TENANT_1, java.util.Set.of(DOUYIN_ACCOUNT_ID),
                        "DOUYIN", null, null, null, null, 1, 20).getTotal());
        var contents = accountAnalysisService.listAccountContents(DOUYIN_ACCOUNT_ID, 1, 50);
        org.junit.jupiter.api.Assertions.assertTrue(contents.getList().stream()
                .anyMatch(item -> "COLLECT".equals(item.getDataSource())
                        && "IT-DISPLAY-DOUYIN-WORK".equals(item.getTitle())));
    }

    @Test
    @DisplayName("M10: 账号分析列表展示采集粉丝数与作品数")
    void accountAnalysisListShowsCollectedMetrics() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/oa/account-analysis/list")
                        .param("platform", "DOUYIN")
                        .param("page", "1")
                        .param("size", "100")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("\"accountId\":9006") || body.contains("\"accountId\": 9006"));
        assertTrue(body.contains("\"followerCount\":12345") || body.contains("\"followerCount\": 12345"));
    }

    @Test
    @DisplayName("M10: 账号详情作品/粉丝端点读取采集表")
    void accountAnalysisDetailShowsCollectedData() throws Exception {
        MvcResult contents = mockMvc.perform(get("/admin-api/oa/account-analysis/" + DOUYIN_ACCOUNT_ID + "/contents")
                        .param("page", "1")
                        .param("size", "50")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String contentsBody = contents.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(contentsBody.contains("IT-DISPLAY-DOUYIN-WORK"));
        assertTrue(contentsBody.contains("COLLECT"));
        assertTrue(contentsBody.contains("\"readCount\":56000") || contentsBody.contains("\"readCount\": 56000"));

        mockMvc.perform(get("/admin-api/oa/account-analysis/" + DOUYIN_ACCOUNT_ID + "/followers")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].followerCount").value(12345));
    }

    @Test
    @DisplayName("M10: 内部内容分析与账号详情读取采集作品")
    void internalContentAndAccountDetailShowCollectedWorks() throws Exception {
        MvcResult internal = mockMvc.perform(get("/admin-api/oa/internal-content/list")
                        .param("platformType", "DOUYIN")
                        .param("page", "1")
                        .param("size", "50")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String internalBody = internal.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(internalBody.contains("IT-DISPLAY-DOUYIN-WORK"));
        assertTrue(internalBody.contains("COLLECT"));

        mockMvc.perform(get("/admin-api/oa/account/get")
                        .param("id", String.valueOf(DOUYIN_ACCOUNT_ID))
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.followerCount").value(12345))
                .andExpect(jsonPath("$.data.workCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("M10: 账号详情趋势端点读取采集数据")
    void accountTrendEndpointsUseCollectedData() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account-analysis/" + DOUYIN_ACCOUNT_ID + "/follower-trend")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].followerCount").value(12345));

        mockMvc.perform(get("/admin-api/oa/account-analysis/" + DOUYIN_ACCOUNT_ID + "/content-trend")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].workCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }
}
