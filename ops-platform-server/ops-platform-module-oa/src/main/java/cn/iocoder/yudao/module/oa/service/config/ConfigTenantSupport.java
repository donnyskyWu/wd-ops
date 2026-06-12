package cn.iocoder.yudao.module.oa.service.config;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;

import java.time.LocalDateTime;

public final class ConfigTenantSupport {

    private ConfigTenantSupport() {
    }

    public static Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return tenantId;
    }

    public static String currentUsername() {
        String username = TenantContextHolder.getUsername();
        return username == null ? "system" : username;
    }

    public static void fillCreate(TenantBaseDO entity) {
        entity.setTenantId(requireTenantId());
        entity.setCreator(currentUsername());
        entity.setUpdater(currentUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
    }

    public static void fillUpdate(TenantBaseDO entity) {
        entity.setUpdater(currentUsername());
        entity.setUpdateTime(LocalDateTime.now());
    }

    public static <T extends TenantBaseDO> T getRequiredInTenant(T entity) {
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!requireTenantId().equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    public static void assertAccountInTenant(AccountMapper accountMapper, Long accountId) {
        if (accountId == null) {
            return;
        }
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null || !requireTenantId().equals(account.getTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "账号不存在");
        }
    }
}
