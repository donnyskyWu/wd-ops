package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.AccountStatusLogDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.KuaishouVideoDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WechatMpArticleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WechatVideoWorkDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.XiaohongshuNoteDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.AccountStatusLogMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.KuaishouVideoMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WechatMpArticleMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WechatVideoWorkMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.XiaohongshuNoteMapper;
import cn.iocoder.yudao.module.oa.service.collect.CollectExecutionResult;
import cn.iocoder.yudao.module.oa.service.collect.CollectExecutionService;
import cn.iocoder.yudao.module.oa.service.collect.unified.UnifiedCollectorAdapter;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M10 P2: WECHAT_OFFICIAL / WECHAT_VIDEO / KUAISHOU / XIAOHONGSHU 多 dataType 路由。
 */
class M10ApiCollectorExecMultiPlatformIT extends OaITBase {

    private static final long TENANT_1 = 1L;
    private static final long MP_ACCOUNT_ID = 9001L;
    private static final long WECHAT_VIDEO_ACCOUNT_ID = 9004L;
    private static final long KUAISHOU_ACCOUNT_ID = 9008L;
    private static final long XIAOHONGSHU_ACCOUNT_ID = 9009L;

    @Autowired
    private UnifiedCollectorAdapter unifiedCollectorAdapter;

    @Autowired
    private CollectExecutionService collectExecutionService;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AccountStatusLogMapper accountStatusLogMapper;

    @Autowired
    private WechatMpArticleMapper wechatMpArticleMapper;

    @Autowired
    private WechatVideoWorkMapper wechatVideoWorkMapper;

    @Autowired
    private KuaishouVideoMapper kuaishouVideoMapper;

    @Autowired
    private XiaohongshuNoteMapper xiaohongshuNoteMapper;

    @Autowired
    private AesUtil aesUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_1);
        TenantContextHolder.setUsername("it-m10-api-multi-platform");
        cleanupArtifacts();
        seedCredentials(WECHAT_VIDEO_ACCOUNT_ID, "sessionid=stub_wv_multi");
        seedCredentials(KUAISHOU_ACCOUNT_ID, "kuaishou.web.cp.api_st=stub");
        seedCredentials(XIAOHONGSHU_ACCOUNT_ID, "web_session=stub_xhs_multi");
        seedWechatOfficialCredentials();
    }

    private void seedWechatOfficialCredentials() {
        AccountDO account = accountMapper.selectById(MP_ACCOUNT_ID);
        account.setCookieEncrypted(aesUtil.encrypt("bizuin=123; data_bizuin=123"));
        account.setMpTokenEncrypted(aesUtil.encrypt("1234567890"));
        accountMapper.updateById(account);
    }

    @AfterEach
    void tearDown() {
        cleanupArtifacts();
        TenantContextHolder.clear();
    }

    private void cleanupArtifacts() {
        for (long accountId : new long[]{MP_ACCOUNT_ID, WECHAT_VIDEO_ACCOUNT_ID, KUAISHOU_ACCOUNT_ID, XIAOHONGSHU_ACCOUNT_ID}) {
            jdbcTemplate.update(
                    "DELETE FROM oa_collect_log WHERE task_id IN (SELECT id FROM oa_collect_task WHERE account_id = ?)",
                    accountId);
            jdbcTemplate.update("DELETE FROM oa_collect_task WHERE account_id = ?", accountId);
            jdbcTemplate.update("DELETE FROM oa_account_status_log WHERE account_id = ? AND stat_date = CURRENT_DATE", accountId);
            jdbcTemplate.update("DELETE FROM oa_collector_account_bind WHERE oa_account_id = ?", accountId);
        }
        jdbcTemplate.update("DELETE FROM oa_wechat_mp_article WHERE account_id = ?", MP_ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM oa_wechat_video_work WHERE account_id = ?", WECHAT_VIDEO_ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM oa_kuaishou_video WHERE account_id = ?", KUAISHOU_ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM oa_xiaohongshu_note WHERE account_id = ?", XIAOHONGSHU_ACCOUNT_ID);
    }

    private void seedCredentials(long accountId, String cookiePlain) {
        AccountDO account = accountMapper.selectById(accountId);
        account.setCookieEncrypted(aesUtil.encrypt(cookiePlain));
        accountMapper.updateById(account);
    }

    @Test
    @DisplayName("M10 P2: MP_FOLLOWER_STATS 路由落库 oa_account_status_log")
    void mpFollowerStatsRoutes() {
        unifiedCollectorAdapter.bindAccount(MP_ACCOUNT_ID);
        CollectExecutionResult result = collectExecutionService.execute(mpTask("MP_FOLLOWER_STATS"));
        assertTrue(result.isSuccess());
        assertEquals(1, result.getRecordCount());
        AccountStatusLogDO log = accountStatusLogMapper.selectOne(new LambdaQueryWrapper<AccountStatusLogDO>()
                .eq(AccountStatusLogDO::getAccountId, MP_ACCOUNT_ID)
                .eq(AccountStatusLogDO::getStatDate, LocalDate.now()));
        assertNotNull(log);
        assertEquals(50_000L, log.getFollowerCount());
    }

    @Test
    @DisplayName("M10 P2: MP_ARTICLE_STATS / CONTENT 路由更新 oa_wechat_mp_article")
    void mpArticleStatsAndContentRoutes() {
        unifiedCollectorAdapter.bindAccount(MP_ACCOUNT_ID);
        collectExecutionService.execute(mpTask("MP_ARTICLE_LIST"));

        CollectExecutionResult stats = collectExecutionService.execute(mpTask("MP_ARTICLE_STATS"));
        assertTrue(stats.isSuccess());
        WechatMpArticleDO article = wechatMpArticleMapper.selectOne(new LambdaQueryWrapper<WechatMpArticleDO>()
                .eq(WechatMpArticleDO::getArticleId, "stub_article_001"));
        assertNotNull(article);
        assertEquals(15_800, article.getReadCount());

        CollectExecutionResult content = collectExecutionService.execute(mpTask("MP_ARTICLE_CONTENT"));
        assertTrue(content.isSuccess());
        article = wechatMpArticleMapper.selectById(article.getId());
        assertEquals("Stub公众号正文内容", article.getContentText());
    }

    @Test
    @DisplayName("M10 P2: WECHAT_VIDEO_LIST / STATS 路由落库")
    void wechatVideoRoutes() {
        unifiedCollectorAdapter.bindAccount(WECHAT_VIDEO_ACCOUNT_ID);
        CollectExecutionResult list = collectExecutionService.execute(channelTask(
                "WECHAT_VIDEO", WECHAT_VIDEO_ACCOUNT_ID, "WECHAT_CHANNELS_API", "WECHAT_VIDEO_LIST"));
        assertTrue(list.isSuccess());
        assertEquals(1, list.getRecordCount());

        CollectExecutionResult stats = collectExecutionService.execute(channelTask(
                "WECHAT_VIDEO", WECHAT_VIDEO_ACCOUNT_ID, "WECHAT_CHANNELS_API", "WECHAT_VIDEO_STATS"));
        assertTrue(stats.isSuccess());
        WechatVideoWorkDO work = wechatVideoWorkMapper.selectOne(new LambdaQueryWrapper<WechatVideoWorkDO>()
                .eq(WechatVideoWorkDO::getVideoId, "stub_wv_video_001"));
        assertEquals(9_200, work.getPlayCount());
    }

    @Test
    @DisplayName("M10 P2: KUAISHOU_VIDEO_LIST / STATS 路由落库")
    void kuaishouVideoRoutes() {
        unifiedCollectorAdapter.bindAccount(KUAISHOU_ACCOUNT_ID);
        collectExecutionService.execute(channelTask(
                "KUAISHOU", KUAISHOU_ACCOUNT_ID, "KUAISHOU_OPEN_API", "KUAISHOU_VIDEO_LIST"));

        CollectExecutionResult stats = collectExecutionService.execute(channelTask(
                "KUAISHOU", KUAISHOU_ACCOUNT_ID, "KUAISHOU_OPEN_API", "KUAISHOU_VIDEO_STATS"));
        assertTrue(stats.isSuccess());
        KuaishouVideoDO video = kuaishouVideoMapper.selectOne(new LambdaQueryWrapper<KuaishouVideoDO>()
                .eq(KuaishouVideoDO::getVideoId, "stub_ks_video_001"));
        assertEquals(18_500, video.getPlayCount());
    }

    @Test
    @DisplayName("M10 P2: XIAOHONGSHU_NOTE_LIST / STATS 路由落库")
    void xiaohongshuNoteRoutes() {
        unifiedCollectorAdapter.bindAccount(XIAOHONGSHU_ACCOUNT_ID);
        collectExecutionService.execute(channelTask(
                "XIAOHONGSHU", XIAOHONGSHU_ACCOUNT_ID, "XIAOHONGSHU_OPEN_API", "XIAOHONGSHU_NOTE_LIST"));

        CollectExecutionResult stats = collectExecutionService.execute(channelTask(
                "XIAOHONGSHU", XIAOHONGSHU_ACCOUNT_ID, "XIAOHONGSHU_OPEN_API", "XIAOHONGSHU_NOTE_STATS"));
        assertTrue(stats.isSuccess());
        XiaohongshuNoteDO note = xiaohongshuNoteMapper.selectOne(new LambdaQueryWrapper<XiaohongshuNoteDO>()
                .eq(XiaohongshuNoteDO::getNoteId, "stub_xhs_note_001"));
        assertEquals(450, note.getLikeCount());
    }

    private CollectTaskDO mpTask(String dataType) {
        CollectTaskDO task = new CollectTaskDO();
        task.setTenantId(TENANT_1);
        task.setPlatformType("WECHAT_OFFICIAL");
        task.setAccountId(MP_ACCOUNT_ID);
        task.setMethod("INTERNAL");
        task.setSource("WECHAT_MP_API");
        task.setDataType(dataType);
        return task;
    }

    private CollectTaskDO channelTask(String platform, long accountId, String source, String dataType) {
        CollectTaskDO task = new CollectTaskDO();
        task.setTenantId(TENANT_1);
        task.setPlatformType(platform);
        task.setAccountId(accountId);
        task.setMethod("INTERNAL");
        task.setSource(source);
        task.setDataType(dataType);
        return task;
    }
}
