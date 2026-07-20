package cn.iocoder.yudao.module.oa.service.account;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.account.FanGroupCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.account.FanGroupRespVO;
import cn.iocoder.yudao.module.oa.api.dto.account.FanGroupUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.PlatformAccountFanGroupDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.account.PlatformAccountFanGroupMapper;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;

import static cn.iocoder.yudao.module.oa.framework.operatelog.OaLogRecordConstants.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformAccountFanGroupServiceImpl implements PlatformAccountFanGroupService {

    private static final Set<String> FAN_GROUP_PLATFORMS = Set.of("DOUYIN", "KUAISHOU");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PlatformAccountFanGroupMapper fanGroupMapper;
    private final AccountMapper accountMapper;

    @Override
    public List<FanGroupRespVO> listByAccount(Long accountId) {
        Long tenantId = requireTenantId();
        assertFanGroupAccount(accountId, tenantId);
        List<PlatformAccountFanGroupDO> records = fanGroupMapper.selectList(
                new LambdaQueryWrapper<PlatformAccountFanGroupDO>()
                        .eq(PlatformAccountFanGroupDO::getTenantId, tenantId)
                        .eq(PlatformAccountFanGroupDO::getAccountId, accountId)
                        .orderByDesc(PlatformAccountFanGroupDO::getId));
        return records.stream().map(this::toResp).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @LogRecord(type = M4_FAN_GROUP_TYPE, subType = M4_FAN_GROUP_CREATE_SUB_TYPE, bizNo = "{{#fanGroup.id}}",
            success = M4_FAN_GROUP_CREATE_SUCCESS)
    public Long create(FanGroupCreateReq req) {
        Long tenantId = requireTenantId();
        assertFanGroupAccount(req.getAccountId(), tenantId);
        assertGroupNameUnique(tenantId, req.getAccountId(), req.getGroupName(), null);

        PlatformAccountFanGroupDO entity = new PlatformAccountFanGroupDO();
        entity.setTenantId(tenantId);
        entity.setAccountId(req.getAccountId());
        entity.setGroupName(req.getGroupName().trim());
        entity.setMemberCount(req.getMemberCount());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        fanGroupMapper.insert(entity);
        LogRecordContext.putVariable("fanGroup", entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @LogRecord(type = M4_FAN_GROUP_TYPE, subType = M4_FAN_GROUP_UPDATE_SUB_TYPE, bizNo = "{{#fanGroup.id}}",
            success = M4_FAN_GROUP_UPDATE_SUCCESS)
    public void update(FanGroupUpdateReq req) {
        PlatformAccountFanGroupDO existing = getRequiredInTenant(req.getId());
        LogRecordContext.putVariable("fanGroup", existing);
        assertGroupNameUnique(existing.getTenantId(), existing.getAccountId(), req.getGroupName(), req.getId());
        existing.setGroupName(req.getGroupName().trim());
        existing.setMemberCount(req.getMemberCount());
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        fanGroupMapper.updateById(existing);
    }

    @Override
    @Transactional
    @LogRecord(type = M4_FAN_GROUP_TYPE, subType = M4_FAN_GROUP_DELETE_SUB_TYPE, bizNo = "{{#fanGroup.id}}",
            success = M4_FAN_GROUP_DELETE_SUCCESS)
    public void delete(Long id) {
        PlatformAccountFanGroupDO existing = getRequiredInTenant(id);
        LogRecordContext.putVariable("fanGroup", existing);
        fanGroupMapper.deleteById(id);
    }

    private FanGroupRespVO toResp(PlatformAccountFanGroupDO entity) {
        FanGroupRespVO vo = new FanGroupRespVO();
        vo.setId(entity.getId());
        vo.setAccountId(entity.getAccountId());
        vo.setGroupName(entity.getGroupName());
        vo.setMemberCount(entity.getMemberCount());
        if (entity.getCreateTime() != null) {
            vo.setCreateTime(entity.getCreateTime().format(DT_FMT));
        }
        return vo;
    }

    private void assertFanGroupAccount(Long accountId, Long tenantId) {
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "关联平台账号不存在");
        }
        if (!tenantId.equals(account.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        if (!FAN_GROUP_PLATFORMS.contains(account.getPlatformType())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "仅抖音/快手账号支持粉丝群管理");
        }
    }

    private void assertGroupNameUnique(Long tenantId, Long accountId, String groupName, Long excludeId) {
        LambdaQueryWrapper<PlatformAccountFanGroupDO> wrapper = new LambdaQueryWrapper<PlatformAccountFanGroupDO>()
                .eq(PlatformAccountFanGroupDO::getTenantId, tenantId)
                .eq(PlatformAccountFanGroupDO::getAccountId, accountId)
                .eq(PlatformAccountFanGroupDO::getGroupName, groupName.trim());
        if (excludeId != null) {
            wrapper.ne(PlatformAccountFanGroupDO::getId, excludeId);
        }
        if (fanGroupMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY.getCode(), "粉丝群名称已存在");
        }
    }

    private PlatformAccountFanGroupDO getRequiredInTenant(Long id) {
        PlatformAccountFanGroupDO entity = fanGroupMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!requireTenantId().equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return tenantId;
    }
}
