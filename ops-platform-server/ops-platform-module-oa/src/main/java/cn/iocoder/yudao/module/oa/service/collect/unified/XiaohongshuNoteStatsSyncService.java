package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectorAccountBindDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.XiaohongshuNoteDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectorAccountBindMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.XiaohongshuNoteMapper;
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
public class XiaohongshuNoteStatsSyncService {

    private static final String BIND_STATUS_BOUND = "BOUND";

    private final CollectorAccountBindMapper collectorAccountBindMapper;
    private final XiaohongshuNoteMapper xiaohongshuNoteMapper;
    private final XiaohongshuNoteSyncService xiaohongshuNoteSyncService;
    private final UnifiedCollectorApiClient unifiedCollectorApiClient;

    @Transactional
    public int syncNoteStats(Long oaAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        CollectorAccountBindDO bind = requireBoundCollector(oaAccountId, tenantId);

        List<XiaohongshuNoteDO> notes = xiaohongshuNoteMapper.selectList(
                new LambdaQueryWrapper<XiaohongshuNoteDO>()
                        .eq(XiaohongshuNoteDO::getTenantId, tenantId)
                        .eq(XiaohongshuNoteDO::getAccountId, oaAccountId)
                        .orderByDesc(XiaohongshuNoteDO::getPublishedAt));
        if (notes.isEmpty()) {
            int listed = xiaohongshuNoteSyncService.syncNotes(oaAccountId);
            notes = xiaohongshuNoteMapper.selectList(
                    new LambdaQueryWrapper<XiaohongshuNoteDO>()
                            .eq(XiaohongshuNoteDO::getTenantId, tenantId)
                            .eq(XiaohongshuNoteDO::getAccountId, oaAccountId));
            if (notes.isEmpty() && listed == 0) {
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                        "暂无笔记可同步：笔记列表采集未返回数据（常见原因：Cookie 为游客态 guest 或未完整登录），"
                                + "请先修复 Cookie 并成功执行 XIAOHONGSHU_NOTE_LIST");
            }
        }
        if (notes.isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "暂无笔记可同步，请先执行笔记列表采集");
        }

        LocalDateTime now = LocalDateTime.now();
        int synced = 0;
        for (XiaohongshuNoteDO note : notes) {
            if (StrUtil.isBlank(note.getXsecToken())) {
                continue;
            }
            Map<String, Object> payload = unifiedCollectorApiClient.getXiaohongshuNoteStats(
                    bind.getCollectorAccountId(), note.getNoteId(), note.getXsecToken());
            JSONObject stats = JSONUtil.parseObj(JSONUtil.toJsonStr(payload));
            if (applyStats(note, stats, now)) {
                ConfigTenantSupport.fillUpdate(note);
                xiaohongshuNoteMapper.updateById(note);
                synced++;
            }
        }
        return synced;
    }

    private boolean applyStats(XiaohongshuNoteDO entity, JSONObject stats, LocalDateTime now) {
        Integer likeCount = firstInt(stats, "liked_count", "like_count", "likeCount");
        if (likeCount == null) {
            return false;
        }
        entity.setLikeCount(likeCount);
        entity.setCollectCount(firstInt(stats, "collected_count", "collect_count", "collectCount"));
        entity.setCommentCount(firstInt(stats, "comment_count", "commentCount"));
        entity.setShareCount(firstInt(stats, "share_count", "shareCount"));
        String title = stats.getStr("title");
        if (StrUtil.isNotBlank(title)) {
            entity.setTitle(title);
        }
        String desc = stats.getStr("desc");
        if (StrUtil.isNotBlank(desc)) {
            entity.setDescription(desc);
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
