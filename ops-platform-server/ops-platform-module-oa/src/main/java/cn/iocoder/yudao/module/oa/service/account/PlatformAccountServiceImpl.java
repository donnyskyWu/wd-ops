package cn.iocoder.yudao.module.oa.service.account;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountReplaceReq;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountRespVO;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.company.CompanyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.phone.PhoneDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.realname.RealnameDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.simcard.SimCardDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.company.CompanyMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.FollowerDailyMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.phone.PhoneMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.realname.RealnameMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.simcard.SimCardMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.framework.auth.DataScopeSupport;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUserContext;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformAccountServiceImpl implements PlatformAccountService {

    private static final Map<String, String> BINDING_FIELD_LABELS = Map.of(
            "realnameId", "实名人",
            "phoneId", "手机",
            "simCardId", "手机卡");
    private static final String PLATFORM_WECHAT_OFFICIAL = "WECHAT_OFFICIAL";
    private static final String QUALIFICATION_ENTERPRISE = "ENTERPRISE";
    private static final String QUALIFICATION_PERSONAL = "PERSONAL";
    private static final Map<String, String> PLATFORM_LABELS = Map.of(
            "WECHAT_OFFICIAL", "公众号",
            "WECHAT_VIDEO", "视频号",
            "DOUYIN", "抖音",
            "KUAISHOU", "快手",
            "XIAOHONGSHU", "小红书",
            "WEWORK", "企微",
            "WECHAT_PERSONAL", "个微");

    private final AccountMapper accountMapper;
    private final CompanyMapper companyMapper;
    private final RealnameMapper realnameMapper;
    private final PhoneMapper phoneMapper;
    private final SimCardMapper simCardMapper;
    private final IpGroupMapper ipGroupMapper;
    private final FollowerDailyMapper followerDailyMapper;
    private final ContentMapper contentMapper;
    private final SysUserMapper sysUserMapper;
    private final AesUtil aesUtil;

    @Override
    public PageResult<AccountRespVO> list(String platformType, String accountName, Long companyId,
                                          Long realnameId, String status, Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .eq(StrUtil.isNotBlank(platformType), AccountDO::getPlatformType, platformType)
                .eq(companyId != null, AccountDO::getCompanyId, companyId)
                .eq(realnameId != null, AccountDO::getRealnameId, realnameId)
                .eq(StrUtil.isNotBlank(status), AccountDO::getStatus, status)
                .like(StrUtil.isNotBlank(accountName), AccountDO::getAccountName, accountName)
                .orderByDesc(AccountDO::getId);
        DataScopeSupport.applyIpGroupScope(wrapper, AccountDO::getIpGroupId);
        Page<AccountDO> page = accountMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        Map<Long, String> companyNames = loadCompanyNames(page.getRecords());
        Map<Long, String> realNames = loadRealNames(page.getRecords());
        List<AccountRespVO> list = page.getRecords().stream()
                .map(e -> toResp(e, companyNames.get(e.getCompanyId()), realNames.get(e.getRealnameId()), null, null))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public AccountRespVO get(Long id) {
        AccountDO entity = getRequiredInTenant(id);
        assertAccountReadable(entity);
        AccountRespVO vo = toResp(entity,
                loadCompanyName(entity.getCompanyId()),
                loadRealName(entity.getRealnameId()),
                maskPhone(entity.getPhoneId()),
                maskSim(entity.getSimCardId()));
        if (entity.getIpGroupId() != null) {
            IpGroupDO group = ipGroupMapper.selectById(entity.getIpGroupId());
            if (group != null) {
                vo.setIpGroupName(group.getGroupName());
            }
        }
        FollowerDailyDO latestFollower = followerDailyMapper.selectOne(new LambdaQueryWrapper<FollowerDailyDO>()
                .eq(FollowerDailyDO::getTenantId, entity.getTenantId())
                .eq(FollowerDailyDO::getAccountId, entity.getId())
                .orderByDesc(FollowerDailyDO::getStatDate)
                .last("LIMIT 1"));
        vo.setFollowerCount(latestFollower != null && latestFollower.getFollowerCount() != null
                ? latestFollower.getFollowerCount() : 0L);
        long workCount = contentMapper.selectCount(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, entity.getTenantId())
                .eq(ContentDO::getAccountId, entity.getId()));
        vo.setWorkCount(Math.toIntExact(workCount));
        enrichWechatOfficialResp(vo, entity);
        return vo;
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-account", action = "create")
    public Long create(AccountCreateReq req) {
        boolean forceReplace = Boolean.TRUE.equals(req.getForceReplace());
        assertForceReplaceReason(forceReplace, req.getReason());
        Long tenantId = requireTenantId();
        assertPlatformAccountUnique(tenantId, req.getPlatformType(), req.getExternalAccountId(), null);
        assertAssociationFields(req.getPlatformType(), req.getQualificationType(),
                req.getCompanyId(), req.getRealnameId(), tenantId);

        if (req.getCompanyId() != null) {
            assertCompanyEnabled(req.getCompanyId(), tenantId);
        }
        if (req.getRealnameId() != null) {
            assertRealnameEnabled(req.getRealnameId(), tenantId);
        }
        if (req.getIntermediaryId() != null) {
            assertRealnameEnabled(req.getIntermediaryId(), tenantId);
        }
        PhoneDO phone = req.getPhoneId() != null ? assertPhoneEnabled(req.getPhoneId(), tenantId) : null;
        SimCardDO sim = req.getSimCardId() != null ? assertSimEnabled(req.getSimCardId(), tenantId) : null;

        resolveBindingConflicts(buildBindingChecks(req.getRealnameId(), req.getPhoneId(), req.getSimCardId()),
                null, forceReplace, req.getReason());

        AccountDO entity = new AccountDO();
        entity.setTenantId(tenantId);
        entity.setPlatformType(req.getPlatformType());
        entity.setAccountType(req.getAccountType());
        entity.setAccountName(req.getAccountName());
        entity.setExternalAccountId(req.getExternalAccountId());
        entity.setCompanyId(req.getCompanyId());
        entity.setRealnameId(req.getRealnameId());
        entity.setIntermediaryId(req.getIntermediaryId());
        entity.setPhoneId(req.getPhoneId());
        entity.setSimCardId(req.getSimCardId());
        entity.setIpGroupId(req.getIpGroupId());
        if (phone != null) {
            entity.setPhoneNumberHash(phone.getPhoneNumberHash());
        }
        if (StrUtil.isNotBlank(req.getCookie())) {
            entity.setCookieEncrypted(aesUtil.encrypt(req.getCookie()));
        }
        applyWechatOfficialFields(entity, req, tenantId);
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "NORMAL"));
        entity.setLinkedAt(LocalDateTime.now());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        accountMapper.insert(entity);
        incrementBoundCounts(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-account", action = "update")
    public void update(AccountUpdateReq req) {
        AccountDO existing = getRequiredInTenant(req.getId());
        boolean forceReplace = Boolean.TRUE.equals(req.getForceReplace());
        assertForceReplaceReason(forceReplace, req.getReason());

        Long tenantId = existing.getTenantId();
        AccountDO before = snapshotBindings(existing);

        String effectiveQualification = req.getQualificationType() != null
                ? req.getQualificationType() : existing.getQualificationType();
        Long effectiveCompanyId = req.getCompanyId() != null ? req.getCompanyId() : existing.getCompanyId();
        Long effectiveRealnameId = req.getRealnameId() != null ? req.getRealnameId() : existing.getRealnameId();
        assertAssociationFields(existing.getPlatformType(), effectiveQualification,
                effectiveCompanyId, effectiveRealnameId, tenantId);

        if (req.getCompanyId() != null) {
            assertCompanyEnabled(req.getCompanyId(), tenantId);
            existing.setCompanyId(req.getCompanyId());
        }
        Map<String, Long> bindingChecks = new LinkedHashMap<>();
        if (req.getRealnameId() != null) {
            assertRealnameEnabled(req.getRealnameId(), tenantId);
            bindingChecks.put("realnameId", req.getRealnameId());
        }
        if (req.getIntermediaryId() != null) {
            assertRealnameEnabled(req.getIntermediaryId(), tenantId);
            existing.setIntermediaryId(req.getIntermediaryId());
        }
        if (req.getPhoneId() != null) {
            assertPhoneEnabled(req.getPhoneId(), tenantId);
            bindingChecks.put("phoneId", req.getPhoneId());
        }
        if (req.getSimCardId() != null) {
            assertSimEnabled(req.getSimCardId(), tenantId);
            bindingChecks.put("simCardId", req.getSimCardId());
        }
        resolveBindingConflicts(bindingChecks, existing.getId(), forceReplace, req.getReason());
        if (req.getRealnameId() != null) {
            existing.setRealnameId(req.getRealnameId());
        }
        if (req.getPhoneId() != null) {
            PhoneDO phone = assertPhoneEnabled(req.getPhoneId(), tenantId);
            existing.setPhoneId(req.getPhoneId());
            existing.setPhoneNumberHash(phone.getPhoneNumberHash());
        }
        if (req.getSimCardId() != null) {
            existing.setSimCardId(req.getSimCardId());
        }
        if (req.getAccountName() != null) {
            existing.setAccountName(req.getAccountName());
        }
        if (req.getAccountType() != null) {
            existing.setAccountType(req.getAccountType());
        }
        if (req.getIpGroupId() != null) {
            existing.setIpGroupId(req.getIpGroupId());
        }
        if (req.getCookie() != null) {
            existing.setCookieEncrypted(StrUtil.isBlank(req.getCookie()) ? null : aesUtil.encrypt(req.getCookie()));
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        applyWechatOfficialFields(existing, req, tenantId);
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        accountMapper.updateById(existing);
        adjustBoundCounts(before, existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-account", action = "delete")
    public void delete(Long id) {
        AccountDO existing = getRequiredInTenant(id);
        decrementBoundCounts(existing);
        accountMapper.deleteById(id);
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-account", action = "replace")
    public void replace(Long id, AccountReplaceReq req) {
        AccountUpdateReq updateReq = new AccountUpdateReq();
        updateReq.setId(id);
        updateReq.setRealnameId(req.getRealnameId());
        updateReq.setPhoneId(req.getPhoneId());
        updateReq.setSimCardId(req.getSimCardId());
        updateReq.setForceReplace(true);
        updateReq.setReason(req.getReason());
        update(updateReq);
    }

    private void assertForceReplaceReason(boolean forceReplace, String reason) {
        if (forceReplace && (StrUtil.isBlank(reason) || reason.length() < 5 || reason.length() > 200)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "强制替换时必须填写 5-200 字原因");
        }
    }

    private void assertPlatformAccountUnique(Long tenantId, String platformType, String externalId, Long excludeId) {
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .eq(AccountDO::getPlatformType, platformType)
                .eq(AccountDO::getExternalAccountId, externalId);
        if (excludeId != null) {
            wrapper.ne(AccountDO::getId, excludeId);
        }
        if (accountMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY);
        }
    }

    private Map<String, Long> buildBindingChecks(Long realnameId, Long phoneId, Long simCardId) {
        Map<String, Long> bindings = new LinkedHashMap<>();
        bindings.put("realnameId", realnameId);
        bindings.put("phoneId", phoneId);
        bindings.put("simCardId", simCardId);
        return bindings;
    }

    private void resolveBindingConflicts(Map<String, Long> bindings, Long currentAccountId,
                                         boolean forceReplace, String reason) {
        List<BindingConflict> conflicts = new ArrayList<>();
        for (Map.Entry<String, Long> entry : bindings.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            AccountDO occupied = findOccupant(entry.getKey(), entry.getValue(), currentAccountId);
            if (occupied != null) {
                conflicts.add(new BindingConflict(entry.getKey(), entry.getValue(), occupied));
            }
        }
        if (conflicts.isEmpty()) {
            return;
        }
        if (!forceReplace) {
            String detail = conflicts.stream()
                    .map(c -> formatBindingConflictDetail(c.field(), c.entityId(), c.occupied()))
                    .collect(Collectors.joining("、"));
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND.getCode(),
                    "关联资源已被占用：" + detail);
        }
        assertForceReplaceReason(true, reason);
        for (BindingConflict conflict : conflicts) {
            clearBindingField(conflict.occupied(), conflict.field());
            decrementSingleBound(conflict.field(), conflict.entityId());
        }
    }

    private String formatBindingConflictDetail(String field, Long entityId, AccountDO occupied) {
        String resourceLabel = BINDING_FIELD_LABELS.getOrDefault(field, "关联资源");
        String resourceName = loadBindingResourceName(field, entityId);
        String resourcePart = StrUtil.isNotBlank(resourceName)
                ? resourceLabel + "「" + resourceName + "」"
                : resourceLabel + "(ID:" + entityId + ")";
        String platformLabel = formatPlatformLabel(occupied.getPlatformType());
        String accountDesc = StrUtil.blankToDefault(occupied.getAccountName(), occupied.getExternalAccountId());
        return resourcePart + "已被" + platformLabel + "账号「" + accountDesc + "」(ID:" + occupied.getId() + ")占用";
    }

    private String loadBindingResourceName(String field, Long entityId) {
        return switch (field) {
            case "realnameId" -> loadRealName(entityId);
            case "phoneId" -> maskPhone(entityId);
            case "simCardId" -> maskSim(entityId);
            default -> null;
        };
    }

    private String formatPlatformLabel(String platformType) {
        if (StrUtil.isBlank(platformType)) {
            return "平台";
        }
        return PLATFORM_LABELS.getOrDefault(platformType, platformType);
    }

    private record BindingConflict(String field, Long entityId, AccountDO occupied) {
    }

    private void clearBindingField(AccountDO account, String field) {
        LambdaUpdateWrapper<AccountDO> wrapper = new LambdaUpdateWrapper<AccountDO>()
                .eq(AccountDO::getId, account.getId());
        switch (field) {
            case "realnameId" -> wrapper.set(AccountDO::getRealnameId, null);
            case "phoneId" -> wrapper.set(AccountDO::getPhoneId, null).set(AccountDO::getPhoneNumberHash, null);
            case "simCardId" -> wrapper.set(AccountDO::getSimCardId, null);
            default -> throw new IllegalArgumentException(field);
        }
        accountMapper.update(null, wrapper);
    }

    private AccountDO findOccupant(String field, Long entityId, Long excludeAccountId) {
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, requireTenantId());
        switch (field) {
            case "realnameId" -> wrapper.eq(AccountDO::getRealnameId, entityId);
            case "phoneId" -> wrapper.eq(AccountDO::getPhoneId, entityId);
            case "simCardId" -> wrapper.eq(AccountDO::getSimCardId, entityId);
            default -> throw new IllegalArgumentException(field);
        }
        if (excludeAccountId != null) {
            wrapper.ne(AccountDO::getId, excludeAccountId);
        }
        wrapper.last("LIMIT 1");
        return accountMapper.selectOne(wrapper);
    }

    private void assertCompanyEnabled(Long companyId, Long tenantId) {
        CompanyDO company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "关联公司不存在");
        }
        if (!tenantId.equals(company.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        if (!"ENABLED".equals(company.getStatus())) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
    }

    private void assertRealnameEnabled(Long realnameId, Long tenantId) {
        RealnameDO realname = realnameMapper.selectById(realnameId);
        if (realname == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "关联实名人不存在");
        }
        if (!tenantId.equals(realname.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        if (!"ENABLED".equals(realname.getStatus())) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
    }

    private PhoneDO assertPhoneEnabled(Long phoneId, Long tenantId) {
        PhoneDO phone = phoneMapper.selectById(phoneId);
        if (phone == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "关联手机不存在");
        }
        if (!tenantId.equals(phone.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        if (!"ENABLED".equals(phone.getStatus())) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
        return phone;
    }

    private SimCardDO assertSimEnabled(Long simCardId, Long tenantId) {
        SimCardDO sim = simCardMapper.selectById(simCardId);
        if (sim == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "关联手机卡不存在");
        }
        if (!tenantId.equals(sim.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        if (!"ENABLED".equals(sim.getStatus())) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
        return sim;
    }

    private void incrementBoundCounts(AccountDO account) {
        bumpRealnameCount(account.getRealnameId(), 1);
        bumpPhoneCount(account.getPhoneId(), 1);
        bumpSimCount(account.getSimCardId(), 1);
    }

    private void decrementBoundCounts(AccountDO account) {
        bumpRealnameCount(account.getRealnameId(), -1);
        bumpPhoneCount(account.getPhoneId(), -1);
        bumpSimCount(account.getSimCardId(), -1);
    }

    private void decrementSingleBound(String field, Long entityId) {
        switch (field) {
            case "realnameId" -> bumpRealnameCount(entityId, -1);
            case "phoneId" -> bumpPhoneCount(entityId, -1);
            case "simCardId" -> bumpSimCount(entityId, -1);
            default -> { }
        }
    }

    private void adjustBoundCounts(AccountDO before, AccountDO after) {
        if (!Objects.equals(before.getRealnameId(), after.getRealnameId())) {
            bumpRealnameCount(before.getRealnameId(), -1);
            bumpRealnameCount(after.getRealnameId(), 1);
        }
        if (!Objects.equals(before.getPhoneId(), after.getPhoneId())) {
            bumpPhoneCount(before.getPhoneId(), -1);
            bumpPhoneCount(after.getPhoneId(), 1);
        }
        if (!Objects.equals(before.getSimCardId(), after.getSimCardId())) {
            bumpSimCount(before.getSimCardId(), -1);
            bumpSimCount(after.getSimCardId(), 1);
        }
    }

    private void bumpRealnameCount(Long id, int delta) {
        if (id == null || delta == 0) {
            return;
        }
        realnameMapper.update(null, new LambdaUpdateWrapper<RealnameDO>()
                .eq(RealnameDO::getId, id)
                .setSql("account_bound_count = GREATEST(0, IFNULL(account_bound_count,0) + " + delta + ")"));
    }

    private void bumpPhoneCount(Long id, int delta) {
        if (id == null || delta == 0) {
            return;
        }
        phoneMapper.update(null, new LambdaUpdateWrapper<PhoneDO>()
                .eq(PhoneDO::getId, id)
                .setSql("account_bound_count = GREATEST(0, IFNULL(account_bound_count,0) + " + delta + ")"));
    }

    private void bumpSimCount(Long id, int delta) {
        if (id == null || delta == 0) {
            return;
        }
        simCardMapper.update(null, new LambdaUpdateWrapper<SimCardDO>()
                .eq(SimCardDO::getId, id)
                .setSql("account_bound_count = GREATEST(0, IFNULL(account_bound_count,0) + " + delta + ")"));
    }

    private AccountDO snapshotBindings(AccountDO entity) {
        AccountDO copy = new AccountDO();
        copy.setRealnameId(entity.getRealnameId());
        copy.setPhoneId(entity.getPhoneId());
        copy.setSimCardId(entity.getSimCardId());
        return copy;
    }

    private AccountDO getRequiredInTenant(Long id) {
        AccountDO entity = accountMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!requireTenantId().equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    /** BR-006：IP 组数据范围用户仅可查看本组账号；ALL 范围不限制 */
    private void assertAccountReadable(AccountDO entity) {
        LoginUser user = LoginUserContext.get();
        if (user == null || DataScopeSupport.ALL.equals(user.getDataScope())) {
            return;
        }
        if (DataScopeSupport.IP_GROUP.equals(user.getDataScope())) {
            Long scopeIpGroupId = user.getIpGroupId();
            if (scopeIpGroupId != null && !Objects.equals(scopeIpGroupId, entity.getIpGroupId())) {
                throw new ServiceException(OaErrorCodes.FORBIDDEN);
            }
        }
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return tenantId;
    }

    private Map<Long, String> loadCompanyNames(List<AccountDO> records) {
        List<Long> ids = records.stream().map(AccountDO::getCompanyId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return companyMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(CompanyDO::getId, CompanyDO::getCompanyName, (a, b) -> a));
    }

    private Map<Long, String> loadRealNames(List<AccountDO> records) {
        List<Long> ids = records.stream().map(AccountDO::getRealnameId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return realnameMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(RealnameDO::getId, RealnameDO::getRealName, (a, b) -> a));
    }

    private String loadCompanyName(Long id) {
        if (id == null) {
            return null;
        }
        CompanyDO company = companyMapper.selectById(id);
        return company != null ? company.getCompanyName() : null;
    }

    private String loadRealName(Long id) {
        if (id == null) {
            return null;
        }
        RealnameDO realname = realnameMapper.selectById(id);
        return realname != null ? realname.getRealName() : null;
    }

    private String maskPhone(Long phoneId) {
        if (phoneId == null) {
            return null;
        }
        PhoneDO phone = phoneMapper.selectById(phoneId);
        if (phone == null || StrUtil.isBlank(phone.getPhoneNumberEncrypted())) {
            return null;
        }
        try {
            String plain = aesUtil.decrypt(phone.getPhoneNumberEncrypted());
            return plain.length() == 11 ? plain.substring(0, 3) + "****" + plain.substring(7) : "****";
        } catch (Exception ex) {
            return "****";
        }
    }

    private String maskSim(Long simCardId) {
        if (simCardId == null) {
            return null;
        }
        SimCardDO sim = simCardMapper.selectById(simCardId);
        if (sim == null) {
            return null;
        }
        return sim.getIccidEncrypted() != null ? "****" : sim.getPhoneNumberHash();
    }

    private AccountRespVO toResp(AccountDO entity, String companyName, String realName,
                                 String phoneMasked, String simMasked) {
        AccountRespVO vo = new AccountRespVO();
        vo.setId(entity.getId());
        vo.setPlatformType(entity.getPlatformType());
        vo.setAccountType(entity.getAccountType());
        vo.setAccountName(entity.getAccountName());
        vo.setExternalAccountId(entity.getExternalAccountId());
        vo.setCompanyId(entity.getCompanyId());
        vo.setCompanyName(companyName);
        vo.setRealnameId(entity.getRealnameId());
        vo.setRealName(realName);
        vo.setPhoneId(entity.getPhoneId());
        vo.setPhoneNumberMasked(phoneMasked);
        vo.setSimCardId(entity.getSimCardId());
        vo.setSimCardMasked(simMasked);
        vo.setIntermediaryId(entity.getIntermediaryId());
        vo.setIpGroupId(entity.getIpGroupId());
        vo.setStatus(entity.getStatus());
        vo.setHasCookie(StrUtil.isNotBlank(entity.getCookieEncrypted()));
        vo.setPublishEnabled(entity.getPublishEnabled() != null && entity.getPublishEnabled() == 1);
        vo.setLinkedAt(entity.getLinkedAt());
        vo.setCreateTime(entity.getCreateTime());
        vo.setTrademarkName(entity.getTrademarkName());
        vo.setEmail(entity.getEmail());
        vo.setHasPassword(StrUtil.isNotBlank(entity.getPasswordEncrypted()));
        vo.setQualificationType(entity.getQualificationType());
        vo.setUsageStatus(entity.getUsageStatus());
        vo.setOriginalAccountName(entity.getOriginalAccountName());
        vo.setCertExpiryTime(entity.getCertExpiryTime());
        vo.setCertCount(entity.getCertCount());
        vo.setLinkedVideoAccountId(entity.getLinkedVideoAccountId());
        vo.setVideoAccountRegisteredAt(entity.getVideoAccountRegisteredAt());
        vo.setAdminName(entity.getAdminName());
        vo.setAdminUserId(entity.getAdminUserId());
        vo.setHasAdminIdCard(StrUtil.isNotBlank(entity.getAdminIdCardEncrypted()));
        return vo;
    }

    private void enrichWechatOfficialResp(AccountRespVO vo, AccountDO entity) {
        if (!PLATFORM_WECHAT_OFFICIAL.equals(entity.getPlatformType())) {
            return;
        }
        if (entity.getLinkedVideoAccountId() != null) {
            AccountDO video = accountMapper.selectById(entity.getLinkedVideoAccountId());
            if (video != null) {
                vo.setLinkedVideoAccountName(video.getAccountName());
            }
        }
        if (entity.getAdminUserId() != null) {
            SysUserDO admin = sysUserMapper.selectById(entity.getAdminUserId());
            if (admin != null) {
                vo.setAdminUserName(admin.getNickname() != null ? admin.getNickname() : admin.getUsername());
                vo.setAdminPhoneMasked(maskUserPhone(admin.getPhoneEncrypted()));
            }
        }
    }

    private void assertAssociationFields(String platformType, String qualificationType,
                                         Long companyId, Long realnameId, Long tenantId) {
        if (PLATFORM_WECHAT_OFFICIAL.equals(platformType)) {
            if (StrUtil.isBlank(qualificationType)) {
                throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "公众号须选择资质类型");
            }
            if (QUALIFICATION_ENTERPRISE.equals(qualificationType)) {
                if (companyId == null) {
                    throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "企业资质须关联企业");
                }
            } else if (QUALIFICATION_PERSONAL.equals(qualificationType)) {
                if (realnameId == null) {
                    throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "个人资质须关联实名人");
                }
            } else {
                throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID.getCode(), "资质类型不合法");
            }
            return;
        }
        if (companyId == null || realnameId == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "公司和实名人必填");
        }
    }

    private void applyWechatOfficialFields(AccountDO entity, AccountCreateReq req, Long tenantId) {
        if (!PLATFORM_WECHAT_OFFICIAL.equals(req.getPlatformType())) {
            return;
        }
        entity.setTrademarkName(req.getTrademarkName());
        entity.setEmail(req.getEmail());
        if (StrUtil.isNotBlank(req.getPassword())) {
            entity.setPasswordEncrypted(aesUtil.encrypt(req.getPassword()));
        }
        entity.setQualificationType(req.getQualificationType());
        entity.setUsageStatus(req.getUsageStatus());
        entity.setOriginalAccountName(req.getOriginalAccountName());
        entity.setCertExpiryTime(req.getCertExpiryTime());
        if (entity.getCertCount() == null) {
            entity.setCertCount(0);
        }
        if (req.getLinkedVideoAccountId() != null) {
            assertLinkedVideoAccount(req.getLinkedVideoAccountId(), tenantId, entity.getId());
            entity.setLinkedVideoAccountId(req.getLinkedVideoAccountId());
            AccountDO video = accountMapper.selectById(req.getLinkedVideoAccountId());
            entity.setVideoAccountRegisteredAt(resolveVideoAccountRegisteredAt(video));
        }
        if (req.getAdminUserId() != null) {
            assertAdminUserEnabled(req.getAdminUserId(), tenantId);
            entity.setAdminUserId(req.getAdminUserId());
            entity.setAdminName(resolveAdminDisplayName(req.getAdminUserId()));
        }
        if (StrUtil.isNotBlank(req.getAdminIdCard())) {
            entity.setAdminIdCardEncrypted(aesUtil.encrypt(req.getAdminIdCard()));
        }
    }

    private void applyWechatOfficialFields(AccountDO entity, AccountUpdateReq req, Long tenantId) {
        if (!PLATFORM_WECHAT_OFFICIAL.equals(entity.getPlatformType())) {
            return;
        }
        if (req.getTrademarkName() != null) {
            entity.setTrademarkName(req.getTrademarkName());
        }
        if (req.getEmail() != null) {
            entity.setEmail(req.getEmail());
        }
        if (StrUtil.isNotBlank(req.getPassword())) {
            entity.setPasswordEncrypted(aesUtil.encrypt(req.getPassword()));
        }
        if (req.getQualificationType() != null) {
            entity.setQualificationType(req.getQualificationType());
        }
        if (req.getUsageStatus() != null) {
            entity.setUsageStatus(req.getUsageStatus());
        }
        if (req.getOriginalAccountName() != null) {
            entity.setOriginalAccountName(req.getOriginalAccountName());
        }
        if (req.getCertExpiryTime() != null) {
            entity.setCertExpiryTime(req.getCertExpiryTime());
        }
        if (req.getLinkedVideoAccountId() != null) {
            assertLinkedVideoAccount(req.getLinkedVideoAccountId(), tenantId, entity.getId());
            entity.setLinkedVideoAccountId(req.getLinkedVideoAccountId());
            AccountDO video = accountMapper.selectById(req.getLinkedVideoAccountId());
            entity.setVideoAccountRegisteredAt(resolveVideoAccountRegisteredAt(video));
        }
        if (req.getAdminUserId() != null) {
            assertAdminUserEnabled(req.getAdminUserId(), tenantId);
            entity.setAdminUserId(req.getAdminUserId());
            entity.setAdminName(resolveAdminDisplayName(req.getAdminUserId()));
        }
        if (StrUtil.isNotBlank(req.getAdminIdCard())) {
            entity.setAdminIdCardEncrypted(aesUtil.encrypt(req.getAdminIdCard()));
        }
    }

    private void assertLinkedVideoAccount(Long videoAccountId, Long tenantId, Long currentAccountId) {
        AccountDO video = accountMapper.selectById(videoAccountId);
        if (video == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "关联视频号不存在");
        }
        if (!tenantId.equals(video.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        if (!"WECHAT_VIDEO".equals(video.getPlatformType())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "关联账号必须为视频号");
        }
        if (currentAccountId != null && currentAccountId.equals(videoAccountId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "不能关联自身");
        }
    }

    private void assertAdminUserEnabled(Long userId, Long tenantId) {
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "管理员用户不存在");
        }
        if (!tenantId.equals(user.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        if (!"ENABLED".equals(user.getStatus())) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
    }

    private LocalDateTime resolveVideoAccountRegisteredAt(AccountDO video) {
        if (video == null) {
            return null;
        }
        return video.getCreateTime();
    }

    private String resolveAdminDisplayName(Long userId) {
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return user.getNickname() != null ? user.getNickname() : user.getUsername();
    }

    private String maskUserPhone(String encrypted) {
        if (StrUtil.isBlank(encrypted)) {
            return null;
        }
        try {
            String plain = aesUtil.decrypt(encrypted);
            return plain.length() == 11 ? plain.substring(0, 3) + "****" + plain.substring(7) : "****";
        } catch (Exception ex) {
            return "****";
        }
    }
}
