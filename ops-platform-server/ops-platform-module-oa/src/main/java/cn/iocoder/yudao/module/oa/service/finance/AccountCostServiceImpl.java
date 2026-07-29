package cn.iocoder.yudao.module.oa.service.finance;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.finance.AccountCostCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.finance.AccountCostUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.finance.AccountCostVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.finance.AccountCostDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.finance.AccountCostMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.account.WechatOfficialAccountResolver;
import cn.iocoder.yudao.module.oa.service.auth.OpsDataScopeSupport;
import cn.iocoder.yudao.module.oa.service.auth.OpsDataScopeSupport.AccountScopeMode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountCostServiceImpl implements AccountCostService {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");

    private final AccountCostMapper accountCostMapper;
    private final AccountMapper accountMapper;
    private final WechatOfficialAccountResolver wechatOfficialAccountResolver;
    private final OpsDataScopeSupport opsDataScopeSupport;

    @Override
    public PageResult<AccountCostVO> list(Long accountId, String costType, LocalDate startDate, LocalDate endDate,
                                          Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<AccountCostDO> wrapper = new LambdaQueryWrapper<AccountCostDO>()
                .eq(AccountCostDO::getTenantId, tenantId)
                .eq(accountId != null, AccountCostDO::getAccountId, accountId)
                .eq(costType != null, AccountCostDO::getCostType, costType)
                .ge(startDate != null, AccountCostDO::getPayDate, startDate)
                .le(endDate != null, AccountCostDO::getPayDate, endDate)
                .orderByDesc(AccountCostDO::getPayDate);
        opsDataScopeSupport.applyAccountIdIn(wrapper, AccountCostDO::getAccountId, AccountScopeMode.MEMBER_GROUPS);
        Page<AccountCostDO> page = accountCostMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        Map<Long, String> accountNames = loadAccountNames(
                page.getRecords().stream()
                        .map(AccountCostDO::getAccountId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()));
        return new PageResult<>(page.getRecords().stream()
                .map(row -> toVO(row, accountNames.get(row.getAccountId())))
                .collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M5-account-cost", action = "create")
    public Long create(AccountCostCreateReq req) {
        assertAmount(req.getAmount());
        AccountDO account = requireAccount(req.getAccountId());
        AccountCostDO entity = new AccountCostDO();
        entity.setTenantId(account.getTenantId());
        entity.setAccountId(req.getAccountId());
        entity.setCostType(req.getCostType());
        entity.setAmount(req.getAmount());
        entity.setPayMethod(req.getPayMethod());
        entity.setPayDate(req.getPayDate());
        entity.setPeriod(req.getPeriod());
        entity.setRemark(req.getRemark());
        entity.setAttachmentId(req.getAttachmentId());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        accountCostMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M5-account-cost", action = "update")
    public void update(AccountCostUpdateReq req) {
        assertAmount(req.getAmount());
        AccountCostDO existing = getRequired(req.getId());
        requireAccount(req.getAccountId());
        existing.setAccountId(req.getAccountId());
        existing.setCostType(req.getCostType());
        existing.setAmount(req.getAmount());
        existing.setPayMethod(req.getPayMethod());
        existing.setPayDate(req.getPayDate());
        existing.setPeriod(req.getPeriod());
        existing.setRemark(req.getRemark());
        existing.setAttachmentId(req.getAttachmentId());
        existing.setUpdater(TenantContextHolder.getUsername());
        accountCostMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M5-account-cost", action = "delete")
    public void delete(Long id) {
        AccountCostDO existing = getRequired(id);
        accountCostMapper.deleteById(existing.getId());
    }

    private AccountCostDO getRequired(Long id) {
        AccountCostDO entity = accountCostMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!Objects.equals(entity.getTenantId(), requireTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        assertAccountReadable(entity.getAccountId());
        return entity;
    }

    private AccountDO requireAccount(Long accountId) {
        AccountDO account = wechatOfficialAccountResolver.requireTenantAccount(accountId, requireTenantId());
        opsDataScopeSupport.assertAccountReadable(account);
        return account;
    }

    private void assertAccountReadable(Long accountId) {
        if (accountId == null) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "账号不能为空");
        }
        AccountDO account = wechatOfficialAccountResolver.requireTenantAccount(accountId, requireTenantId());
        opsDataScopeSupport.assertAccountReadable(account);
    }

    private void assertAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(MIN_AMOUNT) < 0) {
            throw new ServiceException(OaErrorCodes.FINANCE_AMOUNT_INVALID);
        }
    }

    private Map<Long, String> loadAccountNames(Set<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Long tenantId = requireTenantId();
        Map<Long, String> names = accountMapper.selectBatchIds(accountIds).stream()
                .collect(Collectors.toMap(AccountDO::getId, AccountDO::getAccountName, (a, b) -> a));
        for (Long accountId : accountIds) {
            if (names.containsKey(accountId)) {
                continue;
            }
            wechatOfficialAccountResolver.resolveReadableAccount(accountId, tenantId)
                    .ifPresent(account -> names.put(accountId, account.getAccountName()));
        }
        return names;
    }

    private AccountCostVO toVO(AccountCostDO row, String accountName) {
        AccountCostVO vo = new AccountCostVO();
        vo.setId(row.getId());
        vo.setAccountId(row.getAccountId());
        vo.setAccountName(accountName);
        vo.setCostType(row.getCostType());
        vo.setAmount(row.getAmount());
        vo.setPayMethod(row.getPayMethod());
        vo.setPayDate(row.getPayDate());
        vo.setPeriod(row.getPeriod());
        vo.setRemark(row.getRemark());
        vo.setAttachmentId(row.getAttachmentId());
        return vo;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
