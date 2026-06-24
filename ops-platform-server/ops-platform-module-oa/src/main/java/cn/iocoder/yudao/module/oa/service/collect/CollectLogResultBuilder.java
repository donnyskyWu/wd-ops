package cn.iocoder.yudao.module.oa.service.collect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectLogResultVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.AccountStatusLogDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.DouyinFollowerDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.DouyinVideoDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WechatMpArticleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WechatMpFollowerDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.KuaishouVideoDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WechatVideoWorkDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.XiaohongshuNoteDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WeworkDailyStatsDO;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.AccountStatusLogMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.DouyinFollowerMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.DouyinVideoMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WechatMpArticleMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WechatMpFollowerMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.KuaishouVideoMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WechatVideoWorkMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.XiaohongshuNoteMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WeworkDailyStatsMapper;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 采集日志结果摘要构建（不含凭证/密钥）。
 */
@Component
@RequiredArgsConstructor
public class CollectLogResultBuilder {

    private static final int SAMPLE_LIMIT = 10;

    private static final String DATA_TYPE_MP_FOLLOWER_LIST = "MP_FOLLOWER_LIST";
    private static final String DATA_TYPE_MP_FOLLOWER_STATS = "MP_FOLLOWER_STATS";
    private static final String DATA_TYPE_MP_ARTICLE_LIST = "MP_ARTICLE_LIST";
    private static final String DATA_TYPE_MP_ARTICLE_STATS = "MP_ARTICLE_STATS";
    private static final String DATA_TYPE_MP_ARTICLE_CONTENT = "MP_ARTICLE_CONTENT";
    private static final String DATA_TYPE_DOUYIN_FOLLOWER_LIST = "DOUYIN_FOLLOWER_LIST";
    private static final String DATA_TYPE_DOUYIN_VIDEO_LIST = "DOUYIN_VIDEO_LIST";
    private static final String DATA_TYPE_DOUYIN_VIDEO_STATS = "DOUYIN_VIDEO_STATS";
    private static final String DATA_TYPE_WECHAT_VIDEO_LIST = "WECHAT_VIDEO_LIST";
    private static final String DATA_TYPE_WECHAT_VIDEO_STATS = "WECHAT_VIDEO_STATS";
    private static final String DATA_TYPE_KUAISHOU_VIDEO_LIST = "KUAISHOU_VIDEO_LIST";
    private static final String DATA_TYPE_KUAISHOU_VIDEO_STATS = "KUAISHOU_VIDEO_STATS";
    private static final String DATA_TYPE_XIAOHONGSHU_NOTE_LIST = "XIAOHONGSHU_NOTE_LIST";
    private static final String DATA_TYPE_XIAOHONGSHU_NOTE_STATS = "XIAOHONGSHU_NOTE_STATS";
    private static final String DATA_TYPE_WECOM_DAILY_STATS = "WECOM_DAILY_STATS";
    private static final String SOURCE_WECOM_API = "WECOM_API";
    private static final String PLATFORM_WEWORK = "WEWORK";

    private static final Set<String> CHANNEL_FOLLOWER_SOURCES = Set.of(
            "DOUYIN_OPEN_API", "KUAISHOU_OPEN_API", "WECHAT_CHANNELS_API",
            "XIAOHONGSHU_OPEN_API", "BILIBILI_OPEN_API");

    private final WechatMpFollowerMapper wechatMpFollowerMapper;
    private final WechatMpArticleMapper wechatMpArticleMapper;
    private final DouyinFollowerMapper douyinFollowerMapper;
    private final DouyinVideoMapper douyinVideoMapper;
    private final WechatVideoWorkMapper wechatVideoWorkMapper;
    private final KuaishouVideoMapper kuaishouVideoMapper;
    private final XiaohongshuNoteMapper xiaohongshuNoteMapper;
    private final AccountStatusLogMapper accountStatusLogMapper;
    private final WeworkDailyStatsMapper weworkDailyStatsMapper;

    public String build(CollectTaskDO task, CollectExecutionResult result) {
        if (result != null && result.getTypeOutcomes() != null && result.getTypeOutcomes().size() > 1) {
            return buildMultiType(task, result);
        }
        return build(task, result == null ? 0 : result.getRecordCount());
    }

    public String build(CollectTaskDO task, int recordCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        String dataType = resolveDataType(task);
        payload.put("dataType", dataType);
        payload.put("recordCount", recordCount);

        if (DATA_TYPE_MP_FOLLOWER_LIST.equals(dataType)) {
            buildMpFollowerResult(task, recordCount, payload);
        } else if (DATA_TYPE_MP_FOLLOWER_STATS.equals(dataType)) {
            buildMpFollowerStatsResult(task, recordCount, payload);
        } else if (DATA_TYPE_MP_ARTICLE_LIST.equals(dataType)) {
            buildMpArticleResult(task, recordCount, payload);
        } else if (DATA_TYPE_MP_ARTICLE_STATS.equals(dataType)) {
            buildMpArticleStatsResult(task, recordCount, payload);
        } else if (DATA_TYPE_MP_ARTICLE_CONTENT.equals(dataType)) {
            buildMpArticleContentResult(task, recordCount, payload);
        } else if (DATA_TYPE_DOUYIN_FOLLOWER_LIST.equals(dataType)) {
            buildDouyinFollowerResult(task, recordCount, payload);
        } else if (DATA_TYPE_DOUYIN_VIDEO_LIST.equals(dataType)) {
            buildDouyinVideoResult(task, recordCount, payload);
        } else if (DATA_TYPE_DOUYIN_VIDEO_STATS.equals(dataType)) {
            buildDouyinVideoStatsResult(task, recordCount, payload);
        } else if (DATA_TYPE_WECHAT_VIDEO_LIST.equals(dataType)) {
            buildWechatVideoWorkResult(task, recordCount, payload);
        } else if (DATA_TYPE_WECHAT_VIDEO_STATS.equals(dataType)) {
            buildWechatVideoWorkStatsResult(task, recordCount, payload);
        } else if (DATA_TYPE_KUAISHOU_VIDEO_LIST.equals(dataType)) {
            buildKuaishouVideoResult(task, recordCount, payload);
        } else if (DATA_TYPE_KUAISHOU_VIDEO_STATS.equals(dataType)) {
            buildKuaishouVideoStatsResult(task, recordCount, payload);
        } else if (DATA_TYPE_XIAOHONGSHU_NOTE_LIST.equals(dataType)) {
            buildXiaohongshuNoteResult(task, recordCount, payload);
        } else if (DATA_TYPE_XIAOHONGSHU_NOTE_STATS.equals(dataType)) {
            buildXiaohongshuNoteStatsResult(task, recordCount, payload);
        } else if (DATA_TYPE_WECOM_DAILY_STATS.equals(dataType)) {
            buildWecomStatsResult(task, recordCount, payload);
        } else if (isChannelFollowerStats(task)) {
            buildChannelStatsResult(task, recordCount, payload);
        } else {
            buildGenericResult(recordCount, payload);
        }
        return JSONUtil.toJsonStr(payload);
    }

    private String buildMultiType(CollectTaskDO task, CollectExecutionResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("dataType", CollectPlatformDefaults.DATA_TYPE_ALL);
        payload.put("recordCount", result.getRecordCount());
        payload.put("summary", "全量采集 " + result.getTypeOutcomes().size() + " 类，共 "
                + result.getRecordCount() + " 条");

        List<Map<String, Object>> typeResults = new ArrayList<>();
        for (CollectExecutionResult.TypeOutcome outcome : result.getTypeOutcomes()) {
            CollectTaskDO slice = CollectPlatformDefaults.sliceWithDataType(task, outcome.getDataType());
            Map<String, Object> single = JSONUtil.parseObj(build(slice, outcome.getRecordCount()));
            single.put("success", outcome.isSuccess());
            if (!outcome.isSuccess()) {
                single.put("errorMessage", outcome.getErrorMessage());
            }
            typeResults.add(single);
        }
        payload.put("typeResults", typeResults);
        return JSONUtil.toJsonStr(payload);
    }

    private void buildMpFollowerResult(CollectTaskDO task, int recordCount, Map<String, Object> payload) {
        payload.put("targetTable", "oa_wechat_mp_follower");
        payload.put("targetHint", "数据已写入公众号粉丝表，可在账号分析中查看");
        payload.put("summary", "同步粉丝 " + recordCount + " 条");

        if (task.getAccountId() == null) {
            payload.put("samples", List.of());
            return;
        }
        Long tenantId = ConfigTenantSupport.requireTenantId();
        List<WechatMpFollowerDO> rows = wechatMpFollowerMapper.selectList(
                new LambdaQueryWrapper<WechatMpFollowerDO>()
                        .eq(WechatMpFollowerDO::getTenantId, tenantId)
                        .eq(WechatMpFollowerDO::getAccountId, task.getAccountId())
                        .orderByDesc(WechatMpFollowerDO::getSyncedAt)
                        .last("LIMIT " + SAMPLE_LIMIT));
        payload.put("samples", rows.stream().map(this::toFollowerSample).toList());
    }

    private void buildMpArticleResult(CollectTaskDO task, int recordCount, Map<String, Object> payload) {
        payload.put("targetTable", "oa_wechat_mp_article");
        payload.put("targetHint", "数据已写入公众号图文表");
        payload.put("summary", "同步图文 " + recordCount + " 条");

        if (task.getAccountId() == null) {
            payload.put("samples", List.of());
            return;
        }
        Long tenantId = ConfigTenantSupport.requireTenantId();
        List<WechatMpArticleDO> rows = wechatMpArticleMapper.selectList(
                new LambdaQueryWrapper<WechatMpArticleDO>()
                        .eq(WechatMpArticleDO::getTenantId, tenantId)
                        .eq(WechatMpArticleDO::getAccountId, task.getAccountId())
                        .orderByDesc(WechatMpArticleDO::getSyncedAt)
                        .last("LIMIT " + SAMPLE_LIMIT));
        payload.put("samples", rows.stream().map(this::toArticleSample).toList());
    }

    private void buildMpFollowerStatsResult(CollectTaskDO task, int recordCount, Map<String, Object> payload) {
        payload.put("dataType", DATA_TYPE_MP_FOLLOWER_STATS);
        buildChannelStatsResult(task, recordCount, payload);
    }

    private void buildMpArticleStatsResult(CollectTaskDO task, int recordCount, Map<String, Object> payload) {
        payload.put("targetTable", "oa_wechat_mp_article");
        payload.put("targetHint", "图文互动数据已更新至公众号图文表");
        payload.put("summary", "同步图文明细 " + recordCount + " 条");
        appendMpArticleSamples(task, payload, true, false);
    }

    private void buildMpArticleContentResult(CollectTaskDO task, int recordCount, Map<String, Object> payload) {
        payload.put("targetTable", "oa_wechat_mp_article");
        payload.put("targetHint", "图文正文已更新至公众号图文表 content_text 字段");
        payload.put("summary", "同步图文内容 " + recordCount + " 条");
        appendMpArticleSamples(task, payload, false, true);
    }

    private void appendMpArticleSamples(CollectTaskDO task, Map<String, Object> payload,
                                        boolean statsOnly, boolean contentOnly) {
        if (task.getAccountId() == null) {
            payload.put("samples", List.of());
            return;
        }
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<WechatMpArticleDO> query = new LambdaQueryWrapper<WechatMpArticleDO>()
                .eq(WechatMpArticleDO::getTenantId, tenantId)
                .eq(WechatMpArticleDO::getAccountId, task.getAccountId());
        if (statsOnly) {
            query.isNotNull(WechatMpArticleDO::getStatsSyncedAt)
                    .orderByDesc(WechatMpArticleDO::getStatsSyncedAt);
        } else if (contentOnly) {
            query.isNotNull(WechatMpArticleDO::getContentSyncedAt)
                    .orderByDesc(WechatMpArticleDO::getContentSyncedAt);
        }
        query.last("LIMIT " + SAMPLE_LIMIT);
        List<WechatMpArticleDO> rows = wechatMpArticleMapper.selectList(query);
        payload.put("samples", rows.stream().map(this::toArticleSample).toList());
    }

    private void buildDouyinFollowerResult(CollectTaskDO task, int recordCount, Map<String, Object> payload) {
        payload.put("targetTable", "oa_douyin_follower");
        payload.put("targetHint", "数据已写入抖音粉丝表");
        payload.put("summary", "同步抖音粉丝 " + recordCount + " 条");

        if (task.getAccountId() == null) {
            payload.put("samples", List.of());
            return;
        }
        Long tenantId = ConfigTenantSupport.requireTenantId();
        List<DouyinFollowerDO> rows = douyinFollowerMapper.selectList(
                new LambdaQueryWrapper<DouyinFollowerDO>()
                        .eq(DouyinFollowerDO::getTenantId, tenantId)
                        .eq(DouyinFollowerDO::getAccountId, task.getAccountId())
                        .orderByDesc(DouyinFollowerDO::getSyncedAt)
                        .last("LIMIT " + SAMPLE_LIMIT));
        payload.put("samples", rows.stream().map(this::toDouyinFollowerSample).toList());
    }

    private void buildDouyinVideoResult(CollectTaskDO task, int recordCount, Map<String, Object> payload) {
        payload.put("targetTable", "oa_douyin_video");
        payload.put("targetHint", "数据已写入抖音作品表");
        payload.put("summary", "同步抖音作品 " + recordCount + " 条");

        if (task.getAccountId() == null) {
            payload.put("samples", List.of());
            return;
        }
        Long tenantId = ConfigTenantSupport.requireTenantId();
        List<DouyinVideoDO> rows = douyinVideoMapper.selectList(
                new LambdaQueryWrapper<DouyinVideoDO>()
                        .eq(DouyinVideoDO::getTenantId, tenantId)
                        .eq(DouyinVideoDO::getAccountId, task.getAccountId())
                        .orderByDesc(DouyinVideoDO::getPublishedAt)
                        .last("LIMIT " + SAMPLE_LIMIT));
        payload.put("samples", rows.stream().map(this::toDouyinVideoSample).toList());
    }

    private void buildDouyinVideoStatsResult(CollectTaskDO task, int recordCount, Map<String, Object> payload) {
        payload.put("targetTable", "oa_douyin_video");
        payload.put("targetHint", "作品播放/互动数据已更新至抖音作品表");
        payload.put("summary", "同步抖音作品明细 " + recordCount + " 条");

        if (task.getAccountId() == null) {
            payload.put("samples", List.of());
            return;
        }
        Long tenantId = ConfigTenantSupport.requireTenantId();
        List<DouyinVideoDO> rows = douyinVideoMapper.selectList(
                new LambdaQueryWrapper<DouyinVideoDO>()
                        .eq(DouyinVideoDO::getTenantId, tenantId)
                        .eq(DouyinVideoDO::getAccountId, task.getAccountId())
                        .isNotNull(DouyinVideoDO::getStatsSyncedAt)
                        .orderByDesc(DouyinVideoDO::getStatsSyncedAt)
                        .last("LIMIT " + SAMPLE_LIMIT));
        payload.put("samples", rows.stream().map(this::toDouyinVideoStatsSample).toList());
    }

    private void buildWechatVideoWorkResult(CollectTaskDO task, int recordCount, Map<String, Object> payload) {
        payload.put("targetTable", "oa_wechat_video_work");
        payload.put("targetHint", "数据已写入视频号作品表");
        payload.put("summary", "同步视频号作品 " + recordCount + " 条");
        appendWechatVideoSamples(task, payload, false);
    }

    private void buildWechatVideoWorkStatsResult(CollectTaskDO task, int recordCount, Map<String, Object> payload) {
        payload.put("targetTable", "oa_wechat_video_work");
        payload.put("targetHint", "作品互动数据已更新至视频号作品表");
        payload.put("summary", "同步视频号作品明细 " + recordCount + " 条");
        appendWechatVideoSamples(task, payload, true);
    }

    private void appendWechatVideoSamples(CollectTaskDO task, Map<String, Object> payload, boolean statsOnly) {
        if (task.getAccountId() == null) {
            payload.put("samples", List.of());
            return;
        }
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<WechatVideoWorkDO> query = new LambdaQueryWrapper<WechatVideoWorkDO>()
                .eq(WechatVideoWorkDO::getTenantId, tenantId)
                .eq(WechatVideoWorkDO::getAccountId, task.getAccountId());
        if (statsOnly) {
            query.isNotNull(WechatVideoWorkDO::getStatsSyncedAt)
                    .orderByDesc(WechatVideoWorkDO::getStatsSyncedAt);
        } else {
            query.orderByDesc(WechatVideoWorkDO::getPublishedAt);
        }
        query.last("LIMIT " + SAMPLE_LIMIT);
        payload.put("samples", wechatVideoWorkMapper.selectList(query).stream()
                .map(row -> statsOnly ? toWechatVideoStatsSample(row) : toWechatVideoSample(row)).toList());
    }

    private void buildKuaishouVideoResult(CollectTaskDO task, int recordCount, Map<String, Object> payload) {
        payload.put("targetTable", "oa_kuaishou_video");
        payload.put("targetHint", "数据已写入快手作品表");
        payload.put("summary", "同步快手作品 " + recordCount + " 条");
        appendKuaishouVideoSamples(task, payload, false);
    }

    private void buildKuaishouVideoStatsResult(CollectTaskDO task, int recordCount, Map<String, Object> payload) {
        payload.put("targetTable", "oa_kuaishou_video");
        payload.put("targetHint", "作品互动数据已更新至快手作品表");
        payload.put("summary", "同步快手作品明细 " + recordCount + " 条");
        appendKuaishouVideoSamples(task, payload, true);
    }

    private void appendKuaishouVideoSamples(CollectTaskDO task, Map<String, Object> payload, boolean statsOnly) {
        if (task.getAccountId() == null) {
            payload.put("samples", List.of());
            return;
        }
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<KuaishouVideoDO> query = new LambdaQueryWrapper<KuaishouVideoDO>()
                .eq(KuaishouVideoDO::getTenantId, tenantId)
                .eq(KuaishouVideoDO::getAccountId, task.getAccountId());
        if (statsOnly) {
            query.isNotNull(KuaishouVideoDO::getStatsSyncedAt)
                    .orderByDesc(KuaishouVideoDO::getStatsSyncedAt);
        } else {
            query.orderByDesc(KuaishouVideoDO::getPublishedAt);
        }
        query.last("LIMIT " + SAMPLE_LIMIT);
        payload.put("samples", kuaishouVideoMapper.selectList(query).stream()
                .map(row -> statsOnly ? toKuaishouVideoStatsSample(row) : toKuaishouVideoSample(row)).toList());
    }

    private void buildXiaohongshuNoteResult(CollectTaskDO task, int recordCount, Map<String, Object> payload) {
        payload.put("targetTable", "oa_xiaohongshu_note");
        payload.put("targetHint", "数据已写入小红书笔记表");
        payload.put("summary", "同步小红书笔记 " + recordCount + " 条");
        appendXiaohongshuNoteSamples(task, payload, false);
    }

    private void buildXiaohongshuNoteStatsResult(CollectTaskDO task, int recordCount, Map<String, Object> payload) {
        payload.put("targetTable", "oa_xiaohongshu_note");
        payload.put("targetHint", "笔记互动数据已更新至小红书笔记表");
        payload.put("summary", "同步小红书笔记明细 " + recordCount + " 条");
        appendXiaohongshuNoteSamples(task, payload, true);
    }

    private void appendXiaohongshuNoteSamples(CollectTaskDO task, Map<String, Object> payload, boolean statsOnly) {
        if (task.getAccountId() == null) {
            payload.put("samples", List.of());
            return;
        }
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<XiaohongshuNoteDO> query = new LambdaQueryWrapper<XiaohongshuNoteDO>()
                .eq(XiaohongshuNoteDO::getTenantId, tenantId)
                .eq(XiaohongshuNoteDO::getAccountId, task.getAccountId());
        if (statsOnly) {
            query.isNotNull(XiaohongshuNoteDO::getStatsSyncedAt)
                    .orderByDesc(XiaohongshuNoteDO::getStatsSyncedAt);
        } else {
            query.orderByDesc(XiaohongshuNoteDO::getPublishedAt);
        }
        query.last("LIMIT " + SAMPLE_LIMIT);
        payload.put("samples", xiaohongshuNoteMapper.selectList(query).stream()
                .map(row -> statsOnly ? toXiaohongshuNoteStatsSample(row) : toXiaohongshuNoteSample(row)).toList());
    }

    private Map<String, Object> toDouyinFollowerSample(DouyinFollowerDO row) {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("followerId", row.getFollowerId());
        sample.put("nickname", row.getNickname());
        sample.put("followedAt", row.getFollowedAt() != null ? row.getFollowedAt().toString() : null);
        return sample;
    }

    private Map<String, Object> toDouyinVideoSample(DouyinVideoDO row) {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("videoId", row.getVideoId());
        sample.put("title", row.getTitle());
        sample.put("publishedAt", row.getPublishedAt() != null ? row.getPublishedAt().toString() : null);
        return sample;
    }

    private Map<String, Object> toDouyinVideoStatsSample(DouyinVideoDO row) {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("videoId", row.getVideoId());
        sample.put("title", row.getTitle());
        sample.put("playCount", row.getPlayCount());
        sample.put("likeCount", row.getLikeCount());
        sample.put("commentCount", row.getCommentCount());
        return sample;
    }

    private Map<String, Object> toWechatVideoSample(WechatVideoWorkDO row) {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("videoId", row.getVideoId());
        sample.put("title", row.getTitle());
        sample.put("publishedAt", row.getPublishedAt() != null ? row.getPublishedAt().toString() : null);
        return sample;
    }

    private Map<String, Object> toWechatVideoStatsSample(WechatVideoWorkDO row) {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("videoId", row.getVideoId());
        sample.put("title", row.getTitle());
        sample.put("playCount", row.getPlayCount());
        sample.put("likeCount", row.getLikeCount());
        return sample;
    }

    private Map<String, Object> toKuaishouVideoSample(KuaishouVideoDO row) {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("videoId", row.getVideoId());
        sample.put("title", row.getTitle());
        sample.put("publishedAt", row.getPublishedAt() != null ? row.getPublishedAt().toString() : null);
        return sample;
    }

    private Map<String, Object> toKuaishouVideoStatsSample(KuaishouVideoDO row) {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("videoId", row.getVideoId());
        sample.put("title", row.getTitle());
        sample.put("playCount", row.getPlayCount());
        sample.put("likeCount", row.getLikeCount());
        return sample;
    }

    private Map<String, Object> toXiaohongshuNoteSample(XiaohongshuNoteDO row) {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("noteId", row.getNoteId());
        sample.put("title", row.getTitle());
        sample.put("publishedAt", row.getPublishedAt() != null ? row.getPublishedAt().toString() : null);
        return sample;
    }

    private Map<String, Object> toXiaohongshuNoteStatsSample(XiaohongshuNoteDO row) {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("noteId", row.getNoteId());
        sample.put("title", row.getTitle());
        sample.put("likeCount", row.getLikeCount());
        sample.put("commentCount", row.getCommentCount());
        return sample;
    }

    private void buildChannelStatsResult(CollectTaskDO task, int recordCount, Map<String, Object> payload) {
        payload.put("dataType", "FOLLOWER_STATS");
        payload.put("targetTable", "oa_account_status_log");
        payload.put("targetHint", "粉丝数已写入账号状态日志，可在账号分析中查看趋势");

        Map<String, Object> metrics = new LinkedHashMap<>();
        if (task.getAccountId() != null) {
            Long tenantId = ConfigTenantSupport.requireTenantId();
            AccountStatusLogDO row = accountStatusLogMapper.selectOne(
                    new LambdaQueryWrapper<AccountStatusLogDO>()
                            .eq(AccountStatusLogDO::getTenantId, tenantId)
                            .eq(AccountStatusLogDO::getAccountId, task.getAccountId())
                            .eq(AccountStatusLogDO::getStatDate, LocalDate.now())
                            .orderByDesc(AccountStatusLogDO::getId)
                            .last("LIMIT 1"));
            if (row != null) {
                metrics.put("statDate", row.getStatDate() != null ? row.getStatDate().toString() : null);
                metrics.put("followerCount", row.getFollowerCount());
                metrics.put("status", row.getStatus());
            }
        }
        payload.put("metrics", metrics);
        Long followerCount = metrics.get("followerCount") instanceof Number number ? number.longValue() : null;
        if (followerCount != null) {
            payload.put("summary", "更新粉丝统计，当前粉丝数 " + followerCount);
        } else {
            payload.put("summary", "更新粉丝统计 " + recordCount + " 条");
        }
        payload.put("samples", List.of());
    }

    private void buildWecomStatsResult(CollectTaskDO task, int recordCount, Map<String, Object> payload) {
        payload.put("targetTable", "oa_wework_daily_stats");
        payload.put("targetHint", "数据已写入企微日统计表，可在企微分析中查看");

        Map<String, Object> metrics = new LinkedHashMap<>();
        if (task.getAccountId() != null) {
            Long tenantId = ConfigTenantSupport.requireTenantId();
            WeworkDailyStatsDO row = weworkDailyStatsMapper.selectOne(
                    new LambdaQueryWrapper<WeworkDailyStatsDO>()
                            .eq(WeworkDailyStatsDO::getTenantId, tenantId)
                            .eq(WeworkDailyStatsDO::getWeworkAccountId, task.getAccountId())
                            .eq(WeworkDailyStatsDO::getStatDate, LocalDate.now())
                            .orderByDesc(WeworkDailyStatsDO::getId)
                            .last("LIMIT 1"));
            if (row != null) {
                metrics.put("statDate", row.getStatDate() != null ? row.getStatDate().toString() : null);
                metrics.put("totalFriends", row.getTotalFriends());
                metrics.put("todayFriendInteractions", row.getTodayFriendInteractions());
                metrics.put("todayMessagesSent", row.getTodayMessagesSent());
            }
        }
        payload.put("metrics", metrics);
        Object totalFriends = metrics.get("totalFriends");
        if (totalFriends != null) {
            payload.put("summary", "同步企微日统计，外部联系人 " + totalFriends + " 人");
        } else {
            payload.put("summary", "同步企微日统计 " + recordCount + " 条");
        }
        payload.put("samples", List.of());
    }

    private void buildGenericResult(int recordCount, Map<String, Object> payload) {
        payload.put("targetHint", recordCount > 0 ? "采集完成，请前往对应业务模块查看" : "执行成功，本次无新增数据");
        payload.put("summary", recordCount > 0 ? "采集成功，共 " + recordCount + " 条记录" : "执行成功，无新增数据");
        payload.put("samples", List.of());
    }

    private Map<String, Object> toFollowerSample(WechatMpFollowerDO row) {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("openid", row.getOpenid());
        sample.put("nickname", row.getNickname());
        sample.put("subscribedAt", row.getSubscribedAt() != null ? row.getSubscribedAt().toString() : null);
        return sample;
    }

    private Map<String, Object> toArticleSample(WechatMpArticleDO row) {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("articleId", row.getArticleId());
        sample.put("title", row.getTitle());
        sample.put("readCount", row.getReadCount());
        sample.put("publishedAt", row.getPublishedAt() != null ? row.getPublishedAt().toString() : null);
        return sample;
    }

    private String resolveDataType(CollectTaskDO task) {
        if (StrUtil.isNotBlank(task.getDataType())) {
            return task.getDataType();
        }
        if (SOURCE_WECOM_API.equals(task.getSource()) && PLATFORM_WEWORK.equals(task.getPlatformType())) {
            return DATA_TYPE_WECOM_DAILY_STATS;
        }
        if ("WECHAT_MP_API".equals(task.getSource())) {
            return DATA_TYPE_MP_FOLLOWER_LIST;
        }
        return task.getDataType();
    }

    private boolean isChannelFollowerStats(CollectTaskDO task) {
        if (DATA_TYPE_MP_FOLLOWER_STATS.equals(task.getDataType())) {
            return true;
        }
        return CHANNEL_FOLLOWER_SOURCES.contains(task.getSource())
                && StrUtil.isBlank(task.getDataType());
    }

    public CollectLogResultVO parse(String resultJson) {
        if (StrUtil.isBlank(resultJson)) {
            return null;
        }
        return JSONUtil.toBean(resultJson, CollectLogResultVO.class);
    }
}
