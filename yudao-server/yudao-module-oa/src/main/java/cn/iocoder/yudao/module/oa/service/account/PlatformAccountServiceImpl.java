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
import cn.iocoder.yudao.module.oa.dal.dataobject.company.CompanyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.phone.PhoneDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.realname.RealnameDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.simcard.SimCardDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformAccountServiceImpl implements PlatformAccountService {

    private final AccountMapper accountMapper;
    private final CompanyMapper companyMapper;
    private final RealnameMapper realnameMapper;
    private final PhoneMapper phoneMapper;
    private final SimCardMapper simCardMapper;
    private final IpGroupMapper ipGroupMapper;
    private final FollowerDailyMapper followerDailyMapper;
    private final ContentMapper contentMapper;
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

        assertCompanyEnabled(req.getCompanyId(), tenantId);
        assertRealnameEnabled(req.getRealnameId(), tenantId);
        if (req.getIntermediaryId() != null) {
            assertRealnameEnabled(req.getIntermediaryId(), tenantId);
        }
        PhoneDO phone = req.getPhoneId() != null ? assertPhoneEnabled(req.getPhoneId(), tenantId) : null;
        SimCardDO sim = req.getSimCardId() != null ? assertSimEnabled(req.getSimCardId(), tenantId) : null;

        resolveBindingConflict("realnameId", req.getRealnameId(), null, forceReplace, req.getReason());
        resolveBindingConflict("phoneId", req.getPhoneId(), null, forceReplace, req.getReason());
        resolveBindingConflict("simCardId", req.getSimCardId(), null, forceReplace, req.getReason());

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

        if (req.getCompanyId() != null) {
            assertCompanyEnabled(req.getCompanyId(), tenantId);
            existing.setCompanyId(req.getCompanyId());
        }
        if (req.getRealnameId() != null) {
            assertRealnameEnabled(req.getRealnameId(), tenantId);
            resolveBindingConflict("realnameId", req.getRealnameId(), existing.getId(), forceReplace, req.getReason());
            existing.setRealnameId(req.getRealnameId());
        }
        if (req.getIntermediaryId() != null) {
            assertRealnameEnabled(req.getIntermediaryId(), tenantId);
            existing.setIntermediaryId(req.getIntermediaryId());
        }
        if (req.getPhoneId() != null) {
            PhoneDO phone = assertPhoneEnabled(req.getPhoneId(), tenantId);
            resolveBindingConflict("phoneId", req.getPhoneId(), existing.getId(), forceReplace, req.getReason());
            existing.setPhoneId(req.getPhoneId());
            existing.setPhoneNumberHash(phone.getPhoneNumberHash());
        }
        if (req.getSimCardId() != null) {
            assertSimEnabled(req.getSimCardId(), tenantId);
            resolveBindingConflict("simCardId", req.getSimCardId(), existing.getId(), forceReplace, req.getReason());
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

    private void resolveBindingConflict(String field, Long entityId, Long currentAccountId,
                                        boolean forceReplace, String reason) {
        if (entityId == null) {
            return;
        }
        AccountDO occupied = findOccupant(field, entityId, currentAccountId);
        if (occupied == null) {
            return;
        }
        if (!forceReplace) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND);
        }
        assertForceReplaceReason(true, reason);
        clearBindingField(occupied, field);
        decrementSingleBound(field, entityId);
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
        vo.setLinkedAt(entity.getLinkedAt());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
