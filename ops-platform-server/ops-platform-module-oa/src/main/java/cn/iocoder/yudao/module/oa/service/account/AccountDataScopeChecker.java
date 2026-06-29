package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.framework.auth.DataScopeSupport;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 平台账号读权限（租户 + IP 组数据范围），与 {@link PlatformAccountServiceImpl#get} 一致。
 */
@Component
@RequiredArgsConstructor
public class AccountDataScopeChecker {

    private final AccountMapper accountMapper;

    public AccountDO requireReadableAccount(Long accountId) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        AccountDO entity = accountMapper.selectById(accountId);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "关联平台账号不存在");
        }
        if (!tenantId.equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        assertAccountReadable(entity);
        return entity;
    }

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
}
