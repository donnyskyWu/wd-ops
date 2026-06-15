package cn.iocoder.yudao.module.oa.service.content.publish;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;

/**
 * 各平台发布 API 适配器（Phase 2：DevStub 实现；M10 接入后按 platformType 注册真实实现）。
 */
public interface PlatformPublishAdapter {

    /** 是否支持该平台类型 */
    boolean supports(String platformType);

    /** 调用平台发布 API */
    PlatformPublishResult publish(ProductionContentDO content, AccountDO account);
}
