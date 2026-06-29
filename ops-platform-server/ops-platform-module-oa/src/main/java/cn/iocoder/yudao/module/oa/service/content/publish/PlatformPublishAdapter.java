package cn.iocoder.yudao.module.oa.service.content.publish;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;

/**
 * 各平台发布 API 适配器（Phase 2：DevStub 实现；M10 接入后按 platformType 注册真实实现）。
 */
public interface PlatformPublishAdapter {

    /** 是否支持该平台类型 */
    boolean supports(String platformType);

    /** 调用平台发布 API（草稿/直发） */
    PlatformPublishResult publish(ProductionContentDO content, AccountDO account);

    /** 是否支持基于草稿 media_id 的正式发布（仅公众号 freepublish/submit） */
    default boolean supportsFormalPublish() {
        return false;
    }

    /** 正式发布（默认不支持） */
    default PlatformPublishResult formalPublish(ProductionContentDO content, AccountDO account, String draftMediaId) {
        return PlatformPublishResult.failure("该平台不支持正式发布");
    }
}
