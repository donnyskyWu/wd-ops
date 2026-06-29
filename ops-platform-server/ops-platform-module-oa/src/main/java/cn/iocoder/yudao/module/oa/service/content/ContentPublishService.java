package cn.iocoder.yudao.module.oa.service.content;

import cn.iocoder.yudao.module.oa.api.dto.content.ContentPublishOptionsVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentPublishReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentPublishResultVO;

public interface ContentPublishService {

    ContentPublishOptionsVO getPublishOptions(Long contentId);

    /** 发布为草稿（公众号 draft/add → PUBLISHED_DRAFT；其他平台 stub → PUBLISHED） */
    ContentPublishResultVO publishToDraft(Long contentId, ContentPublishReq req);

    /** 正式发布（公众号 freepublish/submit → FORMALLY_PUBLISHED） */
    ContentPublishResultVO formalPublish(Long contentId);

    /** @deprecated 兼容旧端点，等同 {@link #publishToDraft} */
    ContentPublishResultVO publish(Long contentId, ContentPublishReq req);
}
