package cn.iocoder.yudao.module.oa.service.content;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.content.KnowledgeCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.KnowledgeLikeReq;
import cn.iocoder.yudao.module.oa.api.dto.content.KnowledgeUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.KnowledgeVO;

public interface KnowledgeBaseService {

    PageResult<KnowledgeVO> list(String title, String category, Integer pageNum, Integer pageSize);

    KnowledgeVO getById(Long id);

    Long create(KnowledgeCreateReq req);

    void update(KnowledgeUpdateReq req);

    void delete(Long id);

    /** P-GATE-UNMOCK-R S-R1 P0-1：like/unlike S-R1 仅触发 updateTime 占位（S-R2 补持久化） */
    void toggleLike(KnowledgeLikeReq req);
}
