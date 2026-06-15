package cn.iocoder.yudao.module.oa.service.content;

import cn.iocoder.yudao.module.oa.api.dto.content.ContentPublishOptionsVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentPublishReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentPublishResultVO;

public interface ContentPublishService {

    ContentPublishOptionsVO getPublishOptions(Long contentId);

    ContentPublishResultVO publish(Long contentId, ContentPublishReq req);
}
