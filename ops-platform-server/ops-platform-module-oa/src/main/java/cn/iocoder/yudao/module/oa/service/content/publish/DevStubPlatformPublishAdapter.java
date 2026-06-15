package cn.iocoder.yudao.module.oa.service.content.publish;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Phase 2 dev stub：模拟各平台发布成功，返回 mock externalId。
 * M10 真实 API 可用后新增 per-platform 实现并优先于本 stub 注册。
 */
@Slf4j
@Component
public class DevStubPlatformPublishAdapter implements PlatformPublishAdapter {

    private static final Set<String> SUPPORTED = Set.of(
            "WECHAT_OFFICIAL", "WECHAT_VIDEO", "DOUYIN", "KUAISHOU", "XIAOHONGSHU");

    @Override
    public boolean supports(String platformType) {
        return StrUtil.isNotBlank(platformType) && SUPPORTED.contains(platformType);
    }

    @Override
    public PlatformPublishResult publish(ProductionContentDO content, AccountDO account) {
        String externalId = "mock-" + account.getPlatformType().toLowerCase() + "-"
                + content.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[DevStub] publish contentId={} accountId={} platform={} externalId={}",
                content.getId(), account.getId(), account.getPlatformType(), externalId);
        return PlatformPublishResult.success(externalId, true);
    }
}
