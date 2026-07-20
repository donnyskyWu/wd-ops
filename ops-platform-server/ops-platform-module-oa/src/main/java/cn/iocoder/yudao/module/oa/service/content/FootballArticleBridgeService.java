package cn.iocoder.yudao.module.oa.service.content;

import cn.iocoder.yudao.module.oa.api.dto.content.FootballSchemeVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;

public interface FootballArticleBridgeService {

    /**
     * OPS create 后双写 Football 草稿（失败不抛，写 ext.football_sync_error）。
     */
    void syncDraftOnCreate(ProductionContentDO content);

    /**
     * OPS update 后 sync 标题与双正文至 author_article。
     */
    void syncOnUpdate(ProductionContentDO content);

    /**
     * 幂等重试 sync（按 production_content_id）。
     */
    FootballSchemeVO retrySync(Long productionContentId);

    FootballSchemeVO getFootballScheme(Long productionContentId);

    FootballSchemeVO shelfOn(Long productionContentId);

    FootballSchemeVO shelfOff(Long productionContentId);
}
