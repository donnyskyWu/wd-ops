package cn.iocoder.yudao.module.oa.service.collect;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncFriendsReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncMessagesReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncMessagesRespVO;
import cn.iocoder.yudao.module.oa.config.CollectProperties;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import cn.iocoder.yudao.module.oa.service.collect.external.ExternalCollectorAdapter;
import cn.iocoder.yudao.module.oa.service.collect.unified.UnifiedCollectorAdapter;
import cn.iocoder.yudao.module.oa.service.collect.unified.UnifiedCollectorApiException;
import cn.iocoder.yudao.module.oa.service.collect.wework.WeComAdapter;
import cn.iocoder.yudao.module.oa.service.collect.wework.WeComApiException;
import cn.iocoder.yudao.module.oa.service.personal.PersonalWechatAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

/**
 * 采集执行入口（M10-COL-S-03 前为 stub；后续切片按 source 路由 Adapter）。
 */
@Service
@RequiredArgsConstructor
public class CollectExecutionService {

    private static final String SOURCE_AOCHUANG = "AOCHUANG_API";
    private static final String SOURCE_WECHAT_MP_API = "WECHAT_MP_API";
    private static final String SOURCE_DOUYIN_OPEN_API = "DOUYIN_OPEN_API";
    private static final String SOURCE_KUAISHOU_OPEN_API = "KUAISHOU_OPEN_API";
    private static final String SOURCE_WECHAT_CHANNELS_API = "WECHAT_CHANNELS_API";
    private static final String SOURCE_XIAOHONGSHU_OPEN_API = "XIAOHONGSHU_OPEN_API";
    private static final String SOURCE_BILIBILI_OPEN_API = "BILIBILI_OPEN_API";
    private static final String SOURCE_WECOM_API = "WECOM_API";
    private static final String SOURCE_UNIFY_COLLECTOR_EXTERNAL = "UNIFY_COLLECTOR_EXTERNAL";
    private static final String METHOD_INTERNAL = "INTERNAL";
    private static final String METHOD_EXTERNAL = "EXTERNAL";
    private static final String PLATFORM_PERSONAL_WECHAT = "WECHAT_PERSONAL";
    private static final String PLATFORM_WECHAT_OFFICIAL = "WECHAT_OFFICIAL";
    private static final String PLATFORM_WECHAT_VIDEO = "WECHAT_VIDEO";
    private static final String PLATFORM_DOUYIN = "DOUYIN";
    private static final String PLATFORM_KUAISHOU = "KUAISHOU";
    private static final String PLATFORM_XIAOHONGSHU = "XIAOHONGSHU";
    private static final String PLATFORM_BILIBILI = "BILIBILI";
    private static final String PLATFORM_WEWORK = "WEWORK";

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
    private static final String DATA_TYPE_EXT_KUAISHOU_USER_VIDEOS = "EXT_KUAISHOU_USER_VIDEOS";

    private final CollectProperties collectProperties;
    private final PersonalWechatAccountService personalWechatAccountService;
    private final UnifiedCollectorAdapter unifiedCollectorAdapter;
    private final ExternalCollectorAdapter externalCollectorAdapter;
    private final WeComAdapter weComAdapter;
    private final CollectExecutionInvoker collectExecutionInvoker;

    public CollectExecutionResult execute(CollectTaskDO task) {
        return executeInternal(task);
    }

    private CollectExecutionResult executeInternal(CollectTaskDO task) {
        if (collectProperties.getStub().isForceFail()) {
            return CollectExecutionResult.failure("模拟采集 API 失败");
        }
        if (SOURCE_AOCHUANG.equals(task.getSource())
                && PLATFORM_PERSONAL_WECHAT.equals(task.getPlatformType())) {
            return executeAochuangPersonalWechat(task);
        }
        if (SOURCE_WECHAT_MP_API.equals(task.getSource())
                && METHOD_INTERNAL.equals(task.getMethod())
                && PLATFORM_WECHAT_OFFICIAL.equals(task.getPlatformType())) {
            return executeWechatMpApi(task);
        }
        if (SOURCE_DOUYIN_OPEN_API.equals(task.getSource())
                && METHOD_INTERNAL.equals(task.getMethod())
                && PLATFORM_DOUYIN.equals(task.getPlatformType())) {
            return executeDouyinOpenApi(task);
        }
        if (SOURCE_KUAISHOU_OPEN_API.equals(task.getSource())
                && METHOD_INTERNAL.equals(task.getMethod())
                && PLATFORM_KUAISHOU.equals(task.getPlatformType())) {
            return executeKuaishouOpenApi(task);
        }
        if (SOURCE_WECHAT_CHANNELS_API.equals(task.getSource())
                && METHOD_INTERNAL.equals(task.getMethod())
                && PLATFORM_WECHAT_VIDEO.equals(task.getPlatformType())) {
            return executeWechatVideoChannelsApi(task);
        }
        if (SOURCE_XIAOHONGSHU_OPEN_API.equals(task.getSource())
                && METHOD_INTERNAL.equals(task.getMethod())
                && PLATFORM_XIAOHONGSHU.equals(task.getPlatformType())) {
            return executeXiaohongshuOpenApi(task);
        }
        if (SOURCE_BILIBILI_OPEN_API.equals(task.getSource())
                && METHOD_INTERNAL.equals(task.getMethod())
                && PLATFORM_BILIBILI.equals(task.getPlatformType())) {
            return executeBilibiliOpenApi(task);
        }
        if (SOURCE_WECOM_API.equals(task.getSource())
                && METHOD_INTERNAL.equals(task.getMethod())
                && PLATFORM_WEWORK.equals(task.getPlatformType())) {
            return executeWeComApi(task);
        }
        if (SOURCE_UNIFY_COLLECTOR_EXTERNAL.equals(task.getSource())
                && METHOD_EXTERNAL.equals(task.getMethod())) {
            return executeExternalCollect(task);
        }
        return CollectExecutionResult.success(0);
    }

    private CollectExecutionResult executeExternalCollect(CollectTaskDO task) {
        if (task.getCollectConfigId() == null) {
            return CollectExecutionResult.failure("外部采集任务未配置竞品账号（collect_config_id）");
        }
        if (task.getAccountId() != null) {
            return CollectExecutionResult.failure("外部采集任务 account_id 必须为空");
        }
        if (PLATFORM_KUAISHOU.equals(task.getPlatformType())) {
            return executeByDataTypes(task, this::executeKuaishouExternalForDataType);
        }
        return CollectExecutionResult.failure("暂不支持的外部采集平台：" + task.getPlatformType());
    }

    private CollectExecutionResult executeKuaishouExternalForDataType(CollectTaskDO task, String dataType) {
        String effective = CollectPlatformDefaults.normalizeSentinel(dataType);
        if (!DATA_TYPE_EXT_KUAISHOU_USER_VIDEOS.equals(effective)) {
            return CollectExecutionResult.failure("暂不支持的快手外部采集类型：" + effective);
        }
        return executeExternalCollectTask(task);
    }

    private CollectExecutionResult executeExternalCollectTask(CollectTaskDO task) {
        try {
            int count = collectExecutionInvoker.invokeExternal(
                    (collectConfigId, dataType) -> externalCollectorAdapter.execute(
                            collectConfigId, dataType, task.getCredentialProfile()),
                    task.getCollectConfigId(),
                    task.getDataType());
            return CollectExecutionResult.success(count);
        } catch (ServiceException ex) {
            return CollectExecutionResult.failure(ex.getMessage());
        } catch (UnifiedCollectorApiException ex) {
            return CollectExecutionResult.failure(ex.getMessage());
        }
    }

    private CollectExecutionResult executeWechatMpApi(CollectTaskDO task) {
        return executeByDataTypes(task, this::executeWechatMpApiForDataType);
    }

    private CollectExecutionResult executeWechatMpApiForDataType(CollectTaskDO task, String dataType) {
        String effective = StrUtil.blankToDefault(dataType, DATA_TYPE_MP_FOLLOWER_STATS);
        if (DATA_TYPE_MP_FOLLOWER_LIST.equals(effective)) {
            return executeUnifiedCollect(task, unifiedCollectorAdapter::executeWechatMpFollowerStatsCollect);
        }
        if (DATA_TYPE_MP_FOLLOWER_STATS.equals(effective)) {
            return executeUnifiedCollect(task, unifiedCollectorAdapter::executeWechatMpFollowerStatsCollect);
        }
        if (DATA_TYPE_MP_ARTICLE_LIST.equals(effective)) {
            return executeUnifiedCollect(task, unifiedCollectorAdapter::executeWechatMpArticleCollect);
        }
        if (DATA_TYPE_MP_ARTICLE_STATS.equals(effective)) {
            return executeUnifiedCollect(task, unifiedCollectorAdapter::executeWechatMpArticleStatsCollect);
        }
        if (DATA_TYPE_MP_ARTICLE_CONTENT.equals(effective)) {
            return executeUnifiedCollect(task, unifiedCollectorAdapter::executeWechatMpArticleContentCollect);
        }
        return executeUnifiedCollect(task, unifiedCollectorAdapter::executeWechatMpFollowerStatsCollect);
    }

    private CollectExecutionResult executeDouyinOpenApi(CollectTaskDO task) {
        return executeByDataTypes(task, this::executeDouyinOpenApiForDataType);
    }

    private CollectExecutionResult executeDouyinOpenApiForDataType(CollectTaskDO task, String dataType) {
        String effective = CollectPlatformDefaults.normalizeSentinel(dataType);
        if (DATA_TYPE_DOUYIN_FOLLOWER_LIST.equals(effective)) {
            return executeUnifiedCollect(task, unifiedCollectorAdapter::executeDouyinFollowerStatsCollect);
        }
        if (DATA_TYPE_DOUYIN_VIDEO_LIST.equals(effective)) {
            return executeUnifiedCollect(task, unifiedCollectorAdapter::executeDouyinVideoListCollect);
        }
        if (DATA_TYPE_DOUYIN_VIDEO_STATS.equals(effective)) {
            return executeUnifiedCollect(task, unifiedCollectorAdapter::executeDouyinVideoStatsCollect);
        }
        return executeUnifiedCollect(task, unifiedCollectorAdapter::executeDouyinFollowerStatsCollect);
    }

    private CollectExecutionResult executeKuaishouOpenApi(CollectTaskDO task) {
        return executeByDataTypes(task, this::executeKuaishouOpenApiForDataType);
    }

    private CollectExecutionResult executeKuaishouOpenApiForDataType(CollectTaskDO task, String dataType) {
        String effective = CollectPlatformDefaults.normalizeSentinel(dataType);
        if (DATA_TYPE_KUAISHOU_VIDEO_LIST.equals(effective)) {
            return executeUnifiedCollect(task, unifiedCollectorAdapter::executeKuaishouVideoListCollect);
        }
        if (DATA_TYPE_KUAISHOU_VIDEO_STATS.equals(effective)) {
            return executeUnifiedCollect(task, unifiedCollectorAdapter::executeKuaishouVideoStatsCollect);
        }
        return executeUnifiedCollect(task, unifiedCollectorAdapter::executeKuaishouFollowerStatsCollect);
    }

    private CollectExecutionResult executeWechatVideoChannelsApi(CollectTaskDO task) {
        return executeByDataTypes(task, this::executeWechatVideoChannelsApiForDataType);
    }

    private CollectExecutionResult executeWechatVideoChannelsApiForDataType(CollectTaskDO task, String dataType) {
        String effective = CollectPlatformDefaults.normalizeSentinel(dataType);
        if (DATA_TYPE_WECHAT_VIDEO_LIST.equals(effective)) {
            return executeUnifiedCollect(task, unifiedCollectorAdapter::executeWechatVideoWorkListCollect);
        }
        if (DATA_TYPE_WECHAT_VIDEO_STATS.equals(effective)) {
            return executeUnifiedCollect(task, unifiedCollectorAdapter::executeWechatVideoWorkStatsCollect);
        }
        return executeUnifiedCollect(task, unifiedCollectorAdapter::executeWechatVideoFollowerStatsCollect);
    }

    private CollectExecutionResult executeXiaohongshuOpenApi(CollectTaskDO task) {
        return executeByDataTypes(task, this::executeXiaohongshuOpenApiForDataType);
    }

    private CollectExecutionResult executeXiaohongshuOpenApiForDataType(CollectTaskDO task, String dataType) {
        String effective = CollectPlatformDefaults.normalizeSentinel(dataType);
        if (DATA_TYPE_XIAOHONGSHU_NOTE_LIST.equals(effective)) {
            return executeUnifiedCollect(task, unifiedCollectorAdapter::executeXiaohongshuNoteListCollect);
        }
        if (DATA_TYPE_XIAOHONGSHU_NOTE_STATS.equals(effective)) {
            return executeUnifiedCollect(task, unifiedCollectorAdapter::executeXiaohongshuNoteStatsCollect);
        }
        return executeUnifiedCollect(task, unifiedCollectorAdapter::executeXiaohongshuFollowerStatsCollect);
    }

    private CollectExecutionResult executeBilibiliOpenApi(CollectTaskDO task) {
        return executeByDataTypes(task, (slice, dataType) ->
                executeUnifiedCollect(slice, unifiedCollectorAdapter::executeBilibiliFollowerStatsCollect));
    }

    private CollectExecutionResult executeWeComApi(CollectTaskDO task) {
        if (task.getAccountId() == null) {
            return CollectExecutionResult.failure("采集任务未配置企微应用");
        }
        List<String> dataTypes = CollectPlatformDefaults.resolveExecutionDataTypes(task);
        if (dataTypes.size() > 1) {
            return executeByDataTypes(task, this::executeWeComApiForDataType);
        }
        String dataType = dataTypes.isEmpty()
                ? resolveWecomDataType(task)
                : dataTypes.get(0);
        return executeWeComApiForDataType(task, dataType);
    }

    private CollectExecutionResult executeWeComApiForDataType(CollectTaskDO task, String dataType) {
        String effective = StrUtil.blankToDefault(dataType, DATA_TYPE_WECOM_DAILY_STATS);
        if (!DATA_TYPE_WECOM_DAILY_STATS.equals(effective)) {
            return CollectExecutionResult.failure("暂不支持的企微采集类型：" + effective);
        }
        try {
            int count = collectExecutionInvoker.invoke(
                    accountId -> weComAdapter.executeDailyStatsCollect(accountId),
                    task.getAccountId());
            return CollectExecutionResult.success(count);
        } catch (ServiceException ex) {
            return CollectExecutionResult.failure(ex.getMessage());
        } catch (WeComApiException ex) {
            return CollectExecutionResult.failure(ex.getMessage());
        }
    }

    private String resolveWecomDataType(CollectTaskDO task) {
        if (StrUtil.isNotBlank(task.getDataType())) {
            return task.getDataType();
        }
        return DATA_TYPE_WECOM_DAILY_STATS;
    }

    private CollectExecutionResult executeByDataTypes(CollectTaskDO task,
                                                      BiFunction<CollectTaskDO, String, CollectExecutionResult> executor) {
        List<String> dataTypes = CollectPlatformDefaults.resolveExecutionDataTypes(task);
        if (dataTypes.isEmpty()) {
            return executor.apply(task, task.getDataType());
        }
        if (dataTypes.size() == 1) {
            return executor.apply(CollectPlatformDefaults.sliceWithDataType(task, dataTypes.get(0)), dataTypes.get(0));
        }
        List<CollectExecutionResult.TypeOutcome> outcomes = new ArrayList<>();
        int totalCount = 0;
        for (String dataType : dataTypes) {
            CollectTaskDO slice = CollectPlatformDefaults.sliceWithDataType(task, dataType);
            CollectExecutionResult single = executor.apply(slice, dataType);
            outcomes.add(CollectExecutionResult.TypeOutcome.of(dataType, single));
            if (single.isSuccess()) {
                totalCount += single.getRecordCount();
            }
        }
        return CollectExecutionResult.aggregate(totalCount, outcomes);
    }

    private CollectExecutionResult executeUnifiedCollect(CollectTaskDO task, ToIntFunction<Long> collector) {
        if (task.getAccountId() == null) {
            return CollectExecutionResult.failure("采集任务未配置平台账号");
        }
        try {
            int count = collectExecutionInvoker.invoke(collector, task.getAccountId());
            return CollectExecutionResult.success(count);
        } catch (ServiceException ex) {
            return CollectExecutionResult.failure(ex.getMessage());
        } catch (UnifiedCollectorApiException ex) {
            return CollectExecutionResult.failure(ex.getMessage());
        }
    }

    private CollectExecutionResult executeAochuangPersonalWechat(CollectTaskDO task) {
        if (task.getAccountId() == null) {
            return CollectExecutionResult.failure("采集任务未配置个微账号");
        }
        try {
            personalWechatAccountService.syncFriends(task.getAccountId(), new PersonalWechatSyncFriendsReq());
            PersonalWechatSyncMessagesRespVO messages = personalWechatAccountService.syncMessages(
                    task.getAccountId(), new PersonalWechatSyncMessagesReq());
            return CollectExecutionResult.success(messages.getSyncedCount());
        } catch (ServiceException ex) {
            return CollectExecutionResult.failure(ex.getMessage());
        }
    }
}

