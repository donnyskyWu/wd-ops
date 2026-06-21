package cn.iocoder.yudao.module.oa.service.collect.aochuang;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.personal.AochuangFriendRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncFriendsReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncFriendsRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.AochuangFriendDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.AochuangSyncCursorDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatAccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.AochuangFriendMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.AochuangSyncCursorMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.PersonalWechatAccountMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.bridging.IdentityResolutionService;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import cn.iocoder.yudao.module.oa.service.personal.PersonalWechatDailyStatsService;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangApiException;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangFriendDTO;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangFriendPageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 奥创好友同步（ADR-045 · M10-AO-S-04）。
 */
@Service
@RequiredArgsConstructor
public class FriendSyncService {

    private static final String SYNC_TYPE_FRIENDS = "FRIENDS";
    private static final int PAGE_SIZE = 100;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AochuangAdapter aochuangAdapter;
    private final AochuangFriendMapper aochuangFriendMapper;
    private final AochuangSyncCursorMapper aochuangSyncCursorMapper;
    private final PersonalWechatAccountMapper personalWechatAccountMapper;
    private final PersonalWechatDailyStatsService dailyStatsService;
    private final IdentityResolutionService identityResolutionService;

    @Transactional
    @AuditLog(module = "M10-friend-sync", action = "sync-friends")
    public PersonalWechatSyncFriendsRespVO syncFriends(Long personalWechatId, PersonalWechatSyncFriendsReq req) {
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

        try {
            do {
                AochuangFriendPageResult page = aochuangAdapter.listFriends(
                        personal.getAochuangAccountRefId(),
                        personal.getAochuangWechatAccountId(),
                        nextCursor,
                        PAGE_SIZE);
                for (AochuangFriendDTO dto : page.getFriends()) {
                    if (StrUtil.isBlank(dto.getFriendId())) {
                        continue;
                    }
                    boolean isNew = upsertFriend(personal, dto, now);
                    synced++;
                    if (isNew) {
                        created++;
                    } else {
                        updated++;
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

        personal.setLastFriendSyncAt(now);
        personal.setUpdater(ConfigTenantSupport.currentUsername());
        personal.setUpdateTime(now);
        personalWechatAccountMapper.updateById(personal);

        int friendCount = aochuangFriendMapper.selectCount(
                new LambdaQueryWrapper<AochuangFriendDO>()
                        .eq(AochuangFriendDO::getTenantId, personal.getTenantId())
                        .eq(AochuangFriendDO::getPersonalWechatId, personal.getId())).intValue();
        dailyStatsService.upsertFriendStats(personal, LocalDate.now(), friendCount);

        PersonalWechatSyncFriendsRespVO resp = new PersonalWechatSyncFriendsRespVO();
        resp.setSyncedCount(synced);
        resp.setCreatedCount(created);
        resp.setUpdatedCount(updated);
        resp.setCompleted(completed);
        return resp;
    }

    public PageResult<AochuangFriendRespVO> listFriends(Long personalWechatId, String nickname,
                                                        Integer pageNo, Integer pageSize) {
        PersonalWechatAccountDO personal = requirePersonalWechat(personalWechatId);
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<AochuangFriendDO> wrapper = new LambdaQueryWrapper<AochuangFriendDO>()
                .eq(AochuangFriendDO::getTenantId, tenantId)
                .eq(AochuangFriendDO::getPersonalWechatId, personal.getId())
                .like(StrUtil.isNotBlank(nickname), AochuangFriendDO::getNickname, nickname)
                .orderByDesc(AochuangFriendDO::getSyncedAt)
                .orderByDesc(AochuangFriendDO::getId);
        Page<AochuangFriendDO> page = aochuangFriendMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<AochuangFriendRespVO> list = page.getRecords().stream()
                .map(this::toResp)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private AochuangSyncCursorDO loadOrCreateCursor(PersonalWechatAccountDO personal, boolean fullSync) {
        Long tenantId = personal.getTenantId();
        AochuangSyncCursorDO cursor = aochuangSyncCursorMapper.selectOne(
                new LambdaQueryWrapper<AochuangSyncCursorDO>()
                        .eq(AochuangSyncCursorDO::getTenantId, tenantId)
                        .eq(AochuangSyncCursorDO::getSyncType, SYNC_TYPE_FRIENDS)
                        .eq(AochuangSyncCursorDO::getAochuangWechatAccountId, personal.getAochuangWechatAccountId())
                        .last("LIMIT 1"));
        if (cursor == null) {
            cursor = new AochuangSyncCursorDO();
            cursor.setSyncType(SYNC_TYPE_FRIENDS);
            cursor.setAochuangWechatAccountId(personal.getAochuangWechatAccountId());
            cursor.setPersonalWechatId(personal.getId());
            ConfigTenantSupport.fillCreate(cursor);
            aochuangSyncCursorMapper.insert(cursor);
        } else if (fullSync) {
            cursor.setCursorValue(null);
        }
        return cursor;
    }

    private boolean upsertFriend(PersonalWechatAccountDO personal, AochuangFriendDTO dto, LocalDateTime now) {
        AochuangFriendDO existing = aochuangFriendMapper.selectOne(
                new LambdaQueryWrapper<AochuangFriendDO>()
                        .eq(AochuangFriendDO::getTenantId, personal.getTenantId())
                        .eq(AochuangFriendDO::getAochuangWechatAccountId, personal.getAochuangWechatAccountId())
                        .eq(AochuangFriendDO::getAochuangFriendId, dto.getFriendId())
                        .last("LIMIT 1"));
        if (existing == null) {
            AochuangFriendDO entity = new AochuangFriendDO();
            entity.setTenantId(personal.getTenantId());
            entity.setPersonalWechatId(personal.getId());
            entity.setAochuangWechatAccountId(personal.getAochuangWechatAccountId());
            applyFriendFields(entity, dto, now);
            ConfigTenantSupport.fillCreate(entity);
            aochuangFriendMapper.insert(entity);
            identityResolutionService.tryResolveAfterFriendUpsert(entity);
            return true;
        }
        applyFriendFields(existing, dto, now);
        existing.setUpdater(ConfigTenantSupport.currentUsername());
        existing.setUpdateTime(now);
        aochuangFriendMapper.updateById(existing);
        identityResolutionService.tryResolveAfterFriendUpsert(existing);
        return false;
    }

    private void applyFriendFields(AochuangFriendDO entity, AochuangFriendDTO dto, LocalDateTime now) {
        entity.setAochuangFriendId(dto.getFriendId());
        entity.setWechatId(dto.getWechatId());
        entity.setAlias(dto.getAlias());
        entity.setNickname(dto.getNickname());
        entity.setAvatar(dto.getAvatar());
        entity.setRemark(dto.getRemark());
        entity.setSyncedAt(now);
    }

    private AochuangFriendRespVO toResp(AochuangFriendDO entity) {
        AochuangFriendRespVO vo = new AochuangFriendRespVO();
        vo.setId(entity.getId());
        vo.setPersonalWechatId(entity.getPersonalWechatId());
        vo.setAochuangWechatAccountId(entity.getAochuangWechatAccountId());
        vo.setAochuangFriendId(entity.getAochuangFriendId());
        vo.setWechatId(entity.getWechatId());
        vo.setAlias(entity.getAlias());
        vo.setNickname(entity.getNickname());
        vo.setAvatar(entity.getAvatar());
        vo.setRemark(entity.getRemark());
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
