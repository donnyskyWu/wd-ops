package cn.iocoder.yudao.module.oa.service.system;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;

/**
 * C-WP0: parallel Football system management endpoints return 410 (D-DEDUP-01 / D-DING-02).
 */
public final class ParallelSystemDeprecatedSupport {

    private ParallelSystemDeprecatedSupport() {
    }

    public static void throwParallelSystemDeprecated() {
        throw new ServiceException(OaErrorCodes.PARALLEL_SYSTEM_CRUD_DEPRECATED);
    }

    public static void throwDingTalkSyncDeprecated() {
        throw new ServiceException(OaErrorCodes.DINGTALK_SYNC_DEPRECATED);
    }
}
