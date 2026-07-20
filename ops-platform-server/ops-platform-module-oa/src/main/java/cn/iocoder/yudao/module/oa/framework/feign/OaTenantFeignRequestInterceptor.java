package cn.iocoder.yudao.module.oa.framework.feign;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Propagate {@link TenantContextHolder} tenant to Football system-server RPC (AL-05).
 * Without this header, system-server MyBatis tenant plugin rejects {@code system_operate_log} inserts.
 */
public class OaTenantFeignRequestInterceptor implements RequestInterceptor {

    public static final String HEADER_TENANT_ID = "tenant-id";

    @Override
    public void apply(RequestTemplate template) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            template.header(HEADER_TENANT_ID, String.valueOf(tenantId));
        }
    }
}
