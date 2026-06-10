package cn.iocoder.yudao.module.oa.service.content;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentReviewReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentVO;

public interface ProductionContentService {

    PageResult<ProductionContentVO> list(String title, String platformType, String contentType,
                                          Long accountId, String status, Integer aiGenerated,
                                          Integer pageNum, Integer pageSize);

    Long create(ProductionContentCreateReq req);

    void update(ProductionContentUpdateReq req);

    void submitReview(Long id);

    void review(Long id, ContentReviewReq req);

    void delete(Long id);
}
