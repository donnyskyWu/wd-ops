package cn.iocoder.yudao.module.oa.service.support;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;

import java.time.LocalDate;
import java.util.UUID;

public final class OaTenantSupport {

    private OaTenantSupport() {
    }

    public static Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }

    public static void requireDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ServiceException(OaErrorCodes.FINANCE_DATE_RANGE_INVALID);
        }
        if (endDate.isBefore(startDate)) {
            throw new ServiceException(OaErrorCodes.FINANCE_DATE_RANGE_INVALID);
        }
    }

    public static ExportJobVO stubExportJob() {
        ExportJobVO vo = new ExportJobVO();
        vo.setJobId(UUID.randomUUID().toString());
        return vo;
    }
}
