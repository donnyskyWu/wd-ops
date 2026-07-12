package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.account.WechatCertRenewalCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.account.WechatCertRenewalRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.OaAccountExtDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.WechatOfficialCertRenewalDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.account.WechatOfficialCertRenewalMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.support.FootballSystemUserValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WechatOfficialCertRenewalServiceImpl implements WechatOfficialCertRenewalService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("300.00");
    private static final String USAGE_RENEWED = "RENEWED";

    private final WechatOfficialCertRenewalMapper renewalMapper;
    private final AccountMapper accountMapper;
    private final OaAccountExtDataService oaAccountExtDataService;
    private final WechatOfficialAccountResolver wechatOfficialAccountResolver;
    private final FootballSystemUserValidator footballSystemUserValidator;

    @Override
    public List<WechatCertRenewalRespVO> listByAccount(Long accountId) {
        Long tenantId = requireTenantId();
        assertWechatOfficialAccount(accountId, tenantId);
        List<WechatOfficialCertRenewalDO> records = renewalMapper.selectList(
                new LambdaQueryWrapper<WechatOfficialCertRenewalDO>()
                        .eq(WechatOfficialCertRenewalDO::getTenantId, tenantId)
                        .eq(WechatOfficialCertRenewalDO::getAccountId, accountId)
                        .orderByDesc(WechatOfficialCertRenewalDO::getRenewalTime)
                        .orderByDesc(WechatOfficialCertRenewalDO::getId));
        Map<Long, String> renewerNames = loadRenewerNames(records);
        return records.stream()
                .map(entity -> toResp(entity, renewerNames.get(entity.getRenewerUserId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-wechat-cert-renewal", action = "create")
    public Long create(WechatCertRenewalCreateReq req) {
        Long tenantId = requireTenantId();
        AccountDO account = assertWechatOfficialAccount(req.getAccountId(), tenantId);
        if (req.getRenewerUserId() != null) {
            assertUserEnabled(req.getRenewerUserId(), tenantId);
        }

        WechatOfficialCertRenewalDO entity = new WechatOfficialCertRenewalDO();
        entity.setTenantId(tenantId);
        entity.setAccountId(req.getAccountId());
        entity.setRenewalTime(req.getRenewalTime());
        entity.setRenewerUserId(req.getRenewerUserId());
        entity.setRenewalAmount(req.getRenewalAmount() != null ? req.getRenewalAmount() : DEFAULT_AMOUNT);
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        renewalMapper.insert(entity);

        applyRenewalSideEffects(req.getAccountId(), tenantId);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-wechat-cert-renewal", action = "delete")
    public void delete(Long id) {
        WechatOfficialCertRenewalDO existing = getRequiredInTenant(id);
        renewalMapper.deleteById(existing.getId());
    }

    private WechatCertRenewalRespVO toResp(WechatOfficialCertRenewalDO entity, String renewerName) {
        WechatCertRenewalRespVO vo = new WechatCertRenewalRespVO();
        vo.setId(entity.getId());
        vo.setAccountId(entity.getAccountId());
        if (entity.getRenewalTime() != null) {
            vo.setRenewalTime(entity.getRenewalTime().format(DT_FMT));
        }
        vo.setRenewerUserId(entity.getRenewerUserId());
        vo.setRenewerName(renewerName);
        vo.setRenewalAmount(entity.getRenewalAmount());
        if (entity.getCreateTime() != null) {
            vo.setCreateTime(entity.getCreateTime().format(DT_FMT));
        }
        return vo;
    }

    private Map<Long, String> loadRenewerNames(List<WechatOfficialCertRenewalDO> records) {
        List<Long> userIds = records.stream()
                .map(WechatOfficialCertRenewalDO::getRenewerUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return footballSystemUserValidator.loadNicknames(userIds);
    }

    private AccountDO assertWechatOfficialAccount(Long accountId, Long tenantId) {
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null) {
            account = wechatOfficialAccountResolver.resolveReadableAccount(accountId, tenantId).orElse(null);
        }
        if (account == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "关联平台账号不存在");
        }
        if (!tenantId.equals(account.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        if (!"WECHAT_OFFICIAL".equals(account.getPlatformType())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "仅公众号账号支持续费认证记录");
        }
        return account;
    }

    private void applyRenewalSideEffects(Long accountId, Long tenantId) {
        AccountDO legacy = accountMapper.selectById(accountId);
        if (legacy != null && "WECHAT_OFFICIAL".equals(legacy.getPlatformType())) {
            int nextCount = (legacy.getCertCount() != null ? legacy.getCertCount() : 0) + 1;
            legacy.setCertCount(nextCount);
            legacy.setUsageStatus(USAGE_RENEWED);
            legacy.setUpdater(TenantContextHolder.getUsername());
            legacy.setUpdateTime(LocalDateTime.now());
            accountMapper.updateById(legacy);
            return;
        }
        OaAccountExtDO ext = oaAccountExtDataService.findByMpAccountId(tenantId, accountId);
        if (ext != null) {
            ext.setUsageStatus(USAGE_RENEWED);
            ext.setUpdater(TenantContextHolder.getUsername());
            ext.setUpdateTime(LocalDateTime.now());
            oaAccountExtDataService.updateById(ext);
        }
    }

    private void assertUserEnabled(Long userId, Long tenantId) {
        footballSystemUserValidator.assertEnabledInTenant(userId, tenantId, "续费人不存在");
    }

    private WechatOfficialCertRenewalDO getRequiredInTenant(Long id) {
        WechatOfficialCertRenewalDO entity = renewalMapper.selectById(id);
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
