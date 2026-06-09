package cn.iocoder.yudao.module.oa.service.content;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.content.KnowledgeCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.KnowledgeVO;

public interface KnowledgeBaseService {

    PageResult<KnowledgeVO> list(String title, String category, Integer pageNum, Integer pageSize);

    Long create(KnowledgeCreateReq req);
}
