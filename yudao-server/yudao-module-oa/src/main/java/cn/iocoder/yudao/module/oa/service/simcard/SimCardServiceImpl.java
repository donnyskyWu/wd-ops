package cn.iocoder.yudao.module.oa.service.simcard;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.simcard.LinkedAccountItemVO;
import cn.iocoder.yudao.module.oa.api.dto.simcard.LinkedAccountsRespVO;
import cn.iocoder.yudao.module.oa.api.dto.simcard.SimCardCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.simcard.SimCardRespVO;
import cn.iocoder.yudao.module.oa.api.dto.simcard.SimCardUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.phone.PhoneDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.simcard.SimCardDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.phone.PhoneMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.simcard.SimCardMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SimCardServiceImpl implements SimCardService {

    private static final Map<String, String> PLATFORM_LABELS = Map.of(
            "WECHAT_OFFICIAL", "公众号",
            "DOUYIN", "抖音",
            "WEWORK", "企业微信",
            "KUAISHOU", "快手",
            "XIAOHONGSHU", "小红书",
            "WECHAT_VIDEO", "视频号"
    );

    private final SimCardMapper simCardMapper;
    private final AccountMapper accountMapper;
    private final PhoneMapper phoneMapper;
    private final SysUserMapper sysUserMapper;
    private final AesUtil aesUtil;

    @Override
    public PageResult<SimCardRespVO> list(String iccid, Long phoneId, String operator, String status,
                                          Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SimCardDO> wrapper = new LambdaQueryWrapper<SimCardDO>()
                .eq(SimCardDO::getTenantId, tenantId)
                .eq(phoneId != null, SimCardDO::getPhoneId, phoneId)
                .eq(StrUtil.isNotBlank(operator), SimCardDO::getOperator, operator)
                .eq(StrUtil.isNotBlank(status), SimCardDO::getStatus, status)
                .orderByDesc(SimCardDO::getId);
        if (StrUtil.isNotBlank(iccid)) {
            wrapper.eq(SimCardDO::getIccidHash, hashPlain(iccid));
        }
        Page<SimCardDO> page = simCardMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        Map<Long, String> userNames = loadUserNames(page.getRecords());
        List<SimCardRespVO> list = page.getRecords().stream()
                .map(entity -> toResp(entity, userNames.get(entity.getAssignedUserId()), loadAccountStats(entity)))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-simcard", action = "create")
    public Long create(SimCardCreateReq req) {
        Long tenantId = requireTenantId();
        assertUserInTenant(req.getAssignedUserId(), tenantId);
        assertPhoneNumberUnique(tenantId, req.getPhoneNumber(), null);

        SimCardDO entity = new SimCardDO();
        entity.setTenantId(tenantId);
        entity.setPhoneId(resolvePhoneId(tenantId, req.getPhoneNumber()));
        entity.setPhoneNumberEncrypted(aesUtil.encrypt(req.getPhoneNumber()));
        entity.setPhoneNumberHash(hashPlain(req.getPhoneNumber()));
        entity.setIsPrimary(StrUtil.blankToDefault(req.getIsPrimary(), "YES"));
        entity.setOperator(req.getOperator());
        entity.setAssignedUserId(req.getAssignedUserId());
        if (StrUtil.isNotBlank(req.getIccid())) {
            entity.setIccidEncrypted(aesUtil.encrypt(req.getIccid()));
            entity.setIccidHash(hashPlain(req.getIccid()));
        }
        entity.setPackageName(req.getPackageName());
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setAccountBoundCount(0);
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        simCardMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-simcard", action = "update")
    public void update(SimCardUpdateReq req) {
        SimCardDO existing = getRequiredInTenant(req.getId());
        if (StrUtil.isNotBlank(req.getPhoneNumber())) {
            assertPhoneNumberUnique(existing.getTenantId(), req.getPhoneNumber(), existing.getId());
            existing.setPhoneNumberEncrypted(aesUtil.encrypt(req.getPhoneNumber()));
            existing.setPhoneNumberHash(hashPlain(req.getPhoneNumber()));
            existing.setPhoneId(resolvePhoneId(existing.getTenantId(), req.getPhoneNumber()));
        }
        if (StrUtil.isNotBlank(req.getIsPrimary())) {
            existing.setIsPrimary(req.getIsPrimary());
        }
        if (StrUtil.isNotBlank(req.getOperator())) {
            existing.setOperator(req.getOperator());
        }
        if (req.getAssignedUserId() != null) {
            assertUserInTenant(req.getAssignedUserId(), existing.getTenantId());
            existing.setAssignedUserId(req.getAssignedUserId());
        }
        if (req.getIccid() != null) {
            if (StrUtil.isBlank(req.getIccid())) {
                existing.setIccidEncrypted(null);
                existing.setIccidHash(null);
            } else {
                existing.setIccidEncrypted(aesUtil.encrypt(req.getIccid()));
                existing.setIccidHash(hashPlain(req.getIccid()));
            }
        }
        if (req.getPackageName() != null) {
            existing.setPackageName(req.getPackageName());
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        simCardMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-simcard", action = "delete")
    public void delete(Long id) {
        SimCardDO existing = getRequiredInTenant(id);
        if (existing.getAccountBoundCount() != null && existing.getAccountBoundCount() > 0) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND);
        }
        simCardMapper.deleteById(id);
    }

    @Override
    public LinkedAccountsRespVO linkedAccounts(Long id, String platformType, String operator) {
        SimCardDO simCard = getRequiredInTenant(id);
        LinkedAccountsRespVO resp = new LinkedAccountsRespVO();
        resp.setPhoneNumberMasked(maskPhone(simCard.getPhoneNumberEncrypted()));
        resp.setOperator(simCard.getOperator());

        if (StrUtil.isNotBlank(operator) && !operator.equals(simCard.getOperator())) {
            resp.setTotalCount(0);
            resp.setAccounts(Collections.emptyList());
            return resp;
        }

        Long tenantId = simCard.getTenantId();
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .eq(StrUtil.isNotBlank(platformType), AccountDO::getPlatformType, platformType)
                .and(w -> {
                    if (simCard.getPhoneId() != null) {
                        w.eq(AccountDO::getPhoneId, simCard.getPhoneId());
                    }
                    w.or().eq(AccountDO::getPhoneNumberHash, simCard.getPhoneNumberHash());
                })
                .orderByDesc(AccountDO::getLinkedAt);

        List<AccountDO> accounts = accountMapper.selectList(wrapper);
        List<LinkedAccountItemVO> items = accounts.stream().map(this::toLinkedItem).collect(Collectors.toList());
        resp.setAccounts(items);
        resp.setTotalCount(items.size());
        return resp;
    }

    private AccountStats loadAccountStats(SimCardDO simCard) {
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, simCard.getTenantId())
                .and(w -> {
                    if (simCard.getPhoneId() != null) {
                        w.eq(AccountDO::getPhoneId, simCard.getPhoneId());
                    }
                    w.or().eq(AccountDO::getPhoneNumberHash, simCard.getPhoneNumberHash());
                });
        List<AccountDO> accounts = accountMapper.selectList(wrapper);
        AccountStats stats = new AccountStats();
        stats.total = accounts.size();
        stats.wechatMp = (int) accounts.stream().filter(a -> "WECHAT_OFFICIAL".equals(a.getPlatformType())).count();
        stats.douyin = (int) accounts.stream().filter(a -> "DOUYIN".equals(a.getPlatformType())).count();
        stats.wework = (int) accounts.stream().filter(a -> "WEWORK".equals(a.getPlatformType())).count();
        return stats;
    }

    private LinkedAccountItemVO toLinkedItem(AccountDO account) {
        LinkedAccountItemVO item = new LinkedAccountItemVO();
        item.setPlatformType(account.getPlatformType());
        item.setPlatformLabel(PLATFORM_LABELS.getOrDefault(account.getPlatformType(), account.getPlatformType()));
        item.setAccountName(account.getAccountName());
        item.setAccountId(account.getExternalAccountId());
        item.setStatus(account.getStatus());
        item.setLinkedAt(account.getLinkedAt());
        return item;
    }

    private Long resolvePhoneId(Long tenantId, String phoneNumber) {
        PhoneDO phone = phoneMapper.selectOne(new LambdaQueryWrapper<PhoneDO>()
                .eq(PhoneDO::getTenantId, tenantId)
                .eq(PhoneDO::getPhoneNumberHash, hashPlain(phoneNumber))
                .last("LIMIT 1"));
        return phone != null ? phone.getId() : null;
    }

    private SimCardDO getRequiredInTenant(Long id) {
        SimCardDO entity = simCardMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        Long tenantId = requireTenantId();
        if (!tenantId.equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private void assertPhoneNumberUnique(Long tenantId, String phoneNumber, Long excludeId) {
        LambdaQueryWrapper<SimCardDO> wrapper = new LambdaQueryWrapper<SimCardDO>()
                .eq(SimCardDO::getTenantId, tenantId)
                .eq(SimCardDO::getPhoneNumberHash, hashPlain(phoneNumber));
        if (excludeId != null) {
            wrapper.ne(SimCardDO::getId, excludeId);
        }
        if (simCardMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY);
        }
    }

    private void assertUserInTenant(Long userId, Long tenantId) {
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "归属人不存在");
        }
        if (!tenantId.equals(user.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
    }

    private Map<Long, String> loadUserNames(List<SimCardDO> records) {
        List<Long> ids = records.stream()
                .map(SimCardDO::getAssignedUserId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUserDO> list = sysUserMapper.selectBatchIds(ids);
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }
        return list.stream().collect(Collectors.toMap(SysUserDO::getId, SysUserDO::getNickname, (a, b) -> a));
    }

    private SimCardRespVO toResp(SimCardDO entity, String assignedUserName, AccountStats stats) {
        SimCardRespVO vo = new SimCardRespVO();
        vo.setId(entity.getId());
        vo.setPhoneId(entity.getPhoneId());
        vo.setPhoneNumberMasked(maskPhone(entity.getPhoneNumberEncrypted()));
        vo.setIsPrimary(entity.getIsPrimary());
        vo.setOperator(entity.getOperator());
        vo.setAssignedUserId(entity.getAssignedUserId());
        vo.setAssignedUserName(assignedUserName);
        vo.setIccidMasked(maskIccid(entity.getIccidEncrypted()));
        vo.setPackageName(entity.getPackageName());
        vo.setStatus(entity.getStatus());
        vo.setTotalLinkedAccounts(stats.total);
        vo.setWechatMpCount(stats.wechatMp);
        vo.setDouyinCount(stats.douyin);
        vo.setWeworkCount(stats.wework);
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private String maskPhone(String encrypted) {
        if (StrUtil.isBlank(encrypted)) {
            return null;
        }
        try {
            String plain = aesUtil.decrypt(encrypted);
            if (plain.length() != 11) {
                return "****";
            }
            return plain.substring(0, 3) + "****" + plain.substring(7);
        } catch (Exception ex) {
            return "****";
        }
    }

    private String maskIccid(String encrypted) {
        if (StrUtil.isBlank(encrypted)) {
            return null;
        }
        try {
            String plain = aesUtil.decrypt(encrypted);
            if (plain.length() <= 8) {
                return "****";
            }
            return plain.substring(0, 4) + "****" + plain.substring(plain.length() - 4);
        } catch (Exception ex) {
            return "****";
        }
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return tenantId;
    }

    private String hashPlain(String plain) {
        return DigestUtil.sha256Hex(plain);
    }

    private static class AccountStats {
        int total;
        int wechatMp;
        int douyin;
        int wework;
    }
}
