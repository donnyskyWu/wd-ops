package cn.iocoder.yudao.module.oa.service.collect.external;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Channel-D · 外部竞品采集 Adapter 实现（ADR-052 · GATE-EXT-P0）。
 */
@Component
@RequiredArgsConstructor
public class ExternalCollectorAdapterImpl implements ExternalCollectorAdapter {

    private static final String DATA_TYPE_EXT_KUAISHOU_USER_VIDEOS = "EXT_KUAISHOU_USER_VIDEOS";

    private final KuaishouExternalWorkSyncService kuaishouExternalWorkSyncService;

    @Override
    public int execute(Long collectConfigId, String dataType, String credentialProfile) {
        if (DATA_TYPE_EXT_KUAISHOU_USER_VIDEOS.equals(dataType)) {
            return kuaishouExternalWorkSyncService.syncUserVideos(collectConfigId, credentialProfile);
        }
        throw new ServiceException(1500, "Channel-D 不支持的外部采集类型：" + dataType);
    }
}
