package cn.iocoder.yudao.module.oa.service.collect.aochuang;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.personal.AochuangMessageRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncMessagesReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncMessagesRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.AochuangMessageDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.AochuangSyncCursorDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatDailyStatsDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.AochuangFriendMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.AochuangMessageMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.AochuangSyncCursorMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.PersonalWechatAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.PersonalWechatDailyStatsMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangApiException;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangMessageDTO;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangMessagePageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 奥创消息同步（ADR-045 · M10-AO-S-05）。
 */
@Service
@RequiredArgsConstructor
public class MessageSyncService {

    private static final String SYNC_TYPE_MESSAGES = "MESSAGES";
    private static final int PAGE_SIZE = 100;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AochuangAdapter aochuangAdapter;
    private final AochuangMessageMapper aochuangMessageMapper;
    private final AochuangFriendMapper aochuangFriendMapper;
    private final AochuangSyncCursorMapper aochuangSyncCursorMapper;
    private final PersonalWechatAccountMapper personalWechatAccountMapper;
    private final PersonalWechatDailyStatsMapper personalWechatDailyStatsMapper;

    @Transactional
    @AuditLog(module = "M10-message-sync", action = "sync-messages")
    public PersonalWechatSyncMessagesRespVO syncMessages(Long personalWechatId, PersonalWechatSyncMessagesReq req) {
        PersonalWechatAccountDO personal = requirePersonalWechat(personalWechatId);
        if (StrUtil.isBlank(personal.getAochuangWechatAccountId())) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "个微未绑定奥创设备，请先同步设备");
        }
        if (personal.getAochuangAccountRefId() == null) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "个微未关联奥创账号");
        }

        aochuangAdapter.requireTenantApi();
        boolean fullSync = req != null && Boolean.TRUE.equals(req.getFullSync());
        AochuangSyncCursorDO cursorRow = loadOrCreateCursor(personal, fullSync);

        int created = 0;
        int updated = 0;
        int synced = 0;
        String nextCursor = fullSync ? null : cursorRow.getCursorValue();
        boolean completed = false;
        LocalDateTime now = LocalDateTime.now();
        Set<LocalDate> affectedDates = new HashSet<>();

        try {
            do {
                AochuangMessagePageResult page = aochuangAdapter.listFriendMessages(
                        personal.getAochuangAccountRefId(),
                        personal.getAochuangWechatAccountId(),
                        nextCursor,
                        PAGE_SIZE);
                for (AochuangMessageDTO dto : page.getMessages()) {
                    if (StrUtil.isBlank(dto.getMessageId())) {
                        continue;
                    }
                    boolean isNew = upsertMessage(personal, dto, now);
                    synced++;
                    if (isNew) {
                        created++;
                    } else {
                        updated++;
                    }
                    if (dto.getMessageTime() != null) {
                        affectedDates.add(dto.getMessageTime().toLocalDate());
                    }
                }
                nextCursor = page.getNextCursor();
                completed = !page.isHasMore();
                if (!completed && StrUtil.isBlank(nextCursor)) {
                    completed = true;
                    break;
                }
            } while (!completed);
        } catch (AochuangApiException ex) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), ex.getMessage());
        }

        cursorRow.setCursorValue(completed ? null : nextCursor);
        cursorRow.setLastSyncAt(now);
        cursorRow.setUpdater(ConfigTenantSupport.currentUsername());
        cursorRow.setUpdateTime(now);
        aochuangSyncCursorMapper.updateById(cursorRow);

        int dailyStatsDays = rebuildDailyStats(personal, affectedDates, now);

        personal.setLastMessageSyncAt(now);
        personal.setUpdater(ConfigTenantSupport.currentUsername());
        personal.setUpdateTime(now);
        personalWechatAccountMapper.updateById(personal);

        PersonalWechatSyncMessagesRespVO resp = new PersonalWechatSyncMessagesRespVO();
        resp.setSyncedCount(synced);
        resp.setCreatedCount(created);
        resp.setUpdatedCount(updated);
        resp.setDailyStatsDays(dailyStatsDays);
        resp.setCompleted(completed);
        return resp;
    }

    public PageResult<AochuangMessageRespVO> listMessages(Long personalWechatId, String aochuangFriendId,
                                                          Integer pageNo, Integer pageSize) {
        PersonalWechatAccountDO personal = requirePersonalWechat(personalWechatId);
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<AochuangMessageDO> wrapper = new LambdaQueryWrapper<AochuangMessageDO>()
                .eq(AochuangMessageDO::getTenantId, tenantId)
                .eq(AochuangMessageDO::getPersonalWechatId, personal.getId())
                .eq(StrUtil.isNotBlank(aochuangFriendId), AochuangMessageDO::getAochuangFriendId, aochuangFriendId)
                .orderByDesc(AochuangMessageDO::getMessageTime)
                .orderByDesc(AochuangMessageDO::getId);
        Page<AochuangMessageDO> page = aochuangMessageMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<AochuangMessageRespVO> list = page.getRecords().stream()
                .map(this::toResp)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private int rebuildDailyStats(PersonalWechatAccountDO personal, Set<LocalDate> affectedDates, LocalDateTime now) {
        if (affectedDates.isEmpty()) {
            return 0;
        }
        Long tenantId = personal.getTenantId();
        int friendCount = aochuangFriendMapper.selectCount(
                new LambdaQueryWrapper<cn.iocoder.yudao.module.oa.dal.dataobject.collect.AochuangFriendDO>()
                        .eq(cn.iocoder.yudao.module.oa.dal.dataobject.collect.AochuangFriendDO::getTenantId, tenantId)
                        .eq(cn.iocoder.yudao.module.oa.dal.dataobject.collect.AochuangFriendDO::getPersonalWechatId,
                                personal.getId())).intValue();
        for (LocalDate statDate : affectedDates) {
            LocalDateTime dayStart = statDate.atStartOfDay();
            LocalDateTime dayEnd = statDate.plusDays(1).atStartOfDay();
            int sent = aochuangMessageMapper.selectCount(
                    new LambdaQueryWrapper<AochuangMessageDO>()
                            .eq(AochuangMessageDO::getTenantId, tenantId)
                            .eq(AochuangMessageDO::getPersonalWechatId, personal.getId())
                            .eq(AochuangMessageDO::getDirection, "SENT")
                            .ge(AochuangMessageDO::getMessageTime, dayStart)
                            .lt(AochuangMessageDO::getMessageTime, dayEnd)).intValue();
            int received = aochuangMessageMapper.selectCount(
                    new LambdaQueryWrapper<AochuangMessageDO>()
                            .eq(AochuangMessageDO::getTenantId, tenantId)
                            .eq(AochuangMessageDO::getPersonalWechatId, personal.getId())
                            .eq(AochuangMessageDO::getDirection, "RECEIVED")
                            .ge(AochuangMessageDO::getMessageTime, dayStart)
                            .lt(AochuangMessageDO::getMessageTime, dayEnd)).intValue();

            PersonalWechatDailyStatsDO existing = personalWechatDailyStatsMapper.selectOne(
                    new LambdaQueryWrapper<PersonalWechatDailyStatsDO>()
                            .eq(PersonalWechatDailyStatsDO::getTenantId, tenantId)
                            .eq(PersonalWechatDailyStatsDO::getPersonalWechatId, personal.getId())
                            .eq(PersonalWechatDailyStatsDO::getStatDate, statDate)
                            .last("LIMIT 1"));
            if (existing == null) {
                PersonalWechatDailyStatsDO entity = new PersonalWechatDailyStatsDO();
                entity.setTenantId(tenantId);
                entity.setPersonalWechatId(personal.getId());
                entity.setStatDate(statDate);
                entity.setTotalFriends(friendCount);
                entity.setNewFriends(0);
                entity.setDeletedFriends(0);
                entity.setMessagesSent(sent);
                entity.setMessagesReceived(received);
                entity.setGroupCount(0);
                ConfigTenantSupport.fillCreate(entity);
                personalWechatDailyStatsMapper.insert(entity);
            } else {
                existing.setTotalFriends(friendCount);
                existing.setMessagesSent(sent);
                existing.setMessagesReceived(received);
                existing.setUpdater(ConfigTenantSupport.currentUsername());
                existing.setUpdateTime(now);
                personalWechatDailyStatsMapper.updateById(existing);
            }
        }
        return affectedDates.size();
    }

    private AochuangSyncCursorDO loadOrCreateCursor(PersonalWechatAccountDO personal, boolean fullSync) {
        Long tenantId = personal.getTenantId();
        AochuangSyncCursorDO cursor = aochuangSyncCursorMapper.selectOne(
                new LambdaQueryWrapper<AochuangSyncCursorDO>()
                        .eq(AochuangSyncCursorDO::getTenantId, tenantId)
                        .eq(AochuangSyncCursorDO::getSyncType, SYNC_TYPE_MESSAGES)
                        .eq(AochuangSyncCursorDO::getAochuangWechatAccountId, personal.getAochuangWechatAccountId())
                        .last("LIMIT 1"));
        if (cursor == null) {
            cursor = new AochuangSyncCursorDO();
            cursor.setSyncType(SYNC_TYPE_MESSAGES);
            cursor.setAochuangWechatAccountId(personal.getAochuangWechatAccountId());
            cursor.setPersonalWechatId(personal.getId());
            ConfigTenantSupport.fillCreate(cursor);
            aochuangSyncCursorMapper.insert(cursor);
        } else if (fullSync) {
            cursor.setCursorValue(null);
        }
        return cursor;
    }

    private boolean upsertMessage(PersonalWechatAccountDO personal, AochuangMessageDTO dto, LocalDateTime now) {
        AochuangMessageDO existing = aochuangMessageMapper.selectOne(
                new LambdaQueryWrapper<AochuangMessageDO>()
                        .eq(AochuangMessageDO::getTenantId, personal.getTenantId())
                        .eq(AochuangMessageDO::getAochuangWechatAccountId, personal.getAochuangWechatAccountId())
                        .eq(AochuangMessageDO::getAochuangMessageId, dto.getMessageId())
                        .last("LIMIT 1"));
        if (existing == null) {
            AochuangMessageDO entity = new AochuangMessageDO();
            entity.setTenantId(personal.getTenantId());
            entity.setPersonalWechatId(personal.getId());
            entity.setAochuangWechatAccountId(personal.getAochuangWechatAccountId());
            applyMessageFields(entity, dto, now);
            ConfigTenantSupport.fillCreate(entity);
            aochuangMessageMapper.insert(entity);
            return true;
        }
        applyMessageFields(existing, dto, now);
        existing.setUpdater(ConfigTenantSupport.currentUsername());
        existing.setUpdateTime(now);
        aochuangMessageMapper.updateById(existing);
        return false;
    }

    private void applyMessageFields(AochuangMessageDO entity, AochuangMessageDTO dto, LocalDateTime now) {
        entity.setAochuangMessageId(dto.getMessageId());
        entity.setAochuangFriendId(dto.getFriendId());
        entity.setMsgType(normalizeMsgType(dto.getMsgType()));
        entity.setDirection(normalizeDirection(dto.getDirection()));
        entity.setContent(dto.getContent());
        entity.setMessageTime(dto.getMessageTime() != null ? dto.getMessageTime() : now);
        entity.setSyncedAt(now);
    }

    private String normalizeDirection(String direction) {
        if (StrUtil.isBlank(direction)) {
            return "RECEIVED";
        }
        String upper = direction.trim().toUpperCase();
        if ("SEND".equals(upper) || "OUT".equals(upper) || "OUTGOING".equals(upper)) {
            return "SENT";
        }
        if ("SENT".equals(upper)) {
            return "SENT";
        }
        return "RECEIVED";
    }

    private String normalizeMsgType(String msgType) {
        if (StrUtil.isBlank(msgType)) {
            return "TEXT";
        }
        String upper = msgType.trim().toUpperCase();
        return switch (upper) {
            case "TEXT", "IMAGE", "VOICE", "VIDEO" -> upper;
            default -> "OTHER";
        };
    }

    private AochuangMessageRespVO toResp(AochuangMessageDO entity) {
        AochuangMessageRespVO vo = new AochuangMessageRespVO();
        vo.setId(entity.getId());
        vo.setPersonalWechatId(entity.getPersonalWechatId());
        vo.setAochuangWechatAccountId(entity.getAochuangWechatAccountId());
        vo.setAochuangMessageId(entity.getAochuangMessageId());
        vo.setAochuangFriendId(entity.getAochuangFriendId());
        vo.setMsgType(entity.getMsgType());
        vo.setDirection(entity.getDirection());
        vo.setContent(entity.getContent());
        if (entity.getMessageTime() != null) {
            vo.setMessageTime(entity.getMessageTime().format(DT_FMT));
        }
        if (entity.getSyncedAt() != null) {
            vo.setSyncedAt(entity.getSyncedAt().format(DT_FMT));
        }
        return vo;
    }

    private PersonalWechatAccountDO requirePersonalWechat(Long id) {
        PersonalWechatAccountDO entity = personalWechatAccountMapper.selectById(id);
        return ConfigTenantSupport.getRequiredInTenant(entity);
    }
}
