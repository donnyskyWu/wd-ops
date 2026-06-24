package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectorAccountBindDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.KuaishouVideoDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectorAccountBindMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.KuaishouVideoMapper;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KuaishouVideoStatsSyncService {

    private static final String BIND_STATUS_BOUND = "BOUND";

    private final CollectorAccountBindMapper collectorAccountBindMapper;
    private final KuaishouVideoMapper kuaishouVideoMapper;
    private final KuaishouVideoSyncService kuaishouVideoSyncService;
    private final UnifiedCollectorApiClient unifiedCollectorApiClient;

    @Transactional
    public int syncVideoStats(Long oaAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        CollectorAccountBindDO bind = requireBoundCollector(oaAccountId, tenantId);

        List<KuaishouVideoDO> videos = kuaishouVideoMapper.selectList(
                new LambdaQueryWrapper<KuaishouVideoDO>()
                        .eq(KuaishouVideoDO::getTenantId, tenantId)
                        .eq(KuaishouVideoDO::getAccountId, oaAccountId)
                        .orderByDesc(KuaishouVideoDO::getPublishedAt));
        if (videos.isEmpty()) {
            kuaishouVideoSyncService.syncVideos(oaAccountId);
            videos = kuaishouVideoMapper.selectList(
                    new LambdaQueryWrapper<KuaishouVideoDO>()
                            .eq(KuaishouVideoDO::getTenantId, tenantId)
                            .eq(KuaishouVideoDO::getAccountId, oaAccountId));
        }
        if (videos.isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "暂无作品可同步，请先执行作品列表采集");
        }

        LocalDateTime now = LocalDateTime.now();
        int synced = 0;
        for (KuaishouVideoDO video : videos) {
            Map<String, Object> payload = unifiedCollectorApiClient.getKuaishouVideoStats(
                    bind.getCollectorAccountId(), video.getVideoUrl(), video.getVideoId());
            JSONObject stats = JSONUtil.parseObj(JSONUtil.toJsonStr(payload));
            if (applyStats(video, stats, now)) {
                ConfigTenantSupport.fillUpdate(video);
                kuaishouVideoMapper.updateById(video);
                synced++;
            }
        }
        return synced;
    }

    private boolean applyStats(KuaishouVideoDO entity, JSONObject stats, LocalDateTime now) {
        Integer playCount = firstInt(stats, "play_count", "view_count", "playCount");
        if (playCount == null) {
            return false;
        }
        entity.setPlayCount(playCount);
        entity.setLikeCount(firstInt(stats, "like_count", "likeCount"));
        entity.setShareCount(firstInt(stats, "share_count", "shareCount"));
        entity.setCommentCount(firstInt(stats, "comment_count", "commentCount"));
        entity.setCollectCount(firstInt(stats, "collect_count", "collectCount"));
        String title = stats.getStr("title");
        if (StrUtil.isNotBlank(title)) {
            entity.setTitle(title);
        }
        entity.setStatsSyncedAt(now);
        return true;
    }

    private CollectorAccountBindDO requireBoundCollector(Long oaAccountId, Long tenantId) {
        CollectorAccountBindDO bind = collectorAccountBindMapper.selectOne(
                new LambdaQueryWrapper<CollectorAccountBindDO>()
                        .eq(CollectorAccountBindDO::getTenantId, tenantId)
                        .eq(CollectorAccountBindDO::getOaAccountId, oaAccountId));
        if (bind == null || StrUtil.isBlank(bind.getCollectorAccountId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "请先绑定 Collector 账号");
        }
        if (!BIND_STATUS_BOUND.equals(bind.getBindStatus())) {
            throw new ServiceException(2022, "Collector 账号未绑定成功，请先完成绑定");
        }
        return bind;
    }

    private Integer firstInt(JSONObject obj, String... keys) {
        for (String key : keys) {
            Object raw = obj.get(key);
            if (raw == null) {
                continue;
            }
            if (raw instanceof Number number) {
                return number.intValue();
            }
            String text = String.valueOf(raw);
            if (StrUtil.isNotBlank(text) && text.chars().allMatch(c -> Character.isDigit(c) || c == '-')) {
                return Integer.parseInt(text);
            }
        }
        return null;
    }
}
