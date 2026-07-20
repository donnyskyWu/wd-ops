package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.service.auth.OpsDataScopeSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 平台账号读权限（租户 + IP 组数据范围），与 {@link PlatformAccountServiceImpl#get} 一致。
 */
@Component
@RequiredArgsConstructor
public class AccountDataScopeChecker {

    private final AccountMapper accountMapper;
    private final WechatOfficialAccountResolver wechatOfficialAccountResolver;
    private final OpsDataScopeSupport opsDataScopeSupport;

    public AccountDO requireReadableAccount(Long accountId) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        AccountDO entity = accountMapper.selectById(accountId);
        if (entity == null) {
            entity = wechatOfficialAccountResolver.resolveReadableAccount(accountId, tenantId).orElse(null);
        }
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "关联平台账号不存在");
        }
        if (!tenantId.equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        opsDataScopeSupport.assertAccountReadable(entity);
        return entity;
    }
}
