package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.ProductivityReviewVO;

public interface ProductivityReviewService {

    PageResult<ProductivityReviewVO> list(Long ipGroupId, Long userId, Integer pageNo, Integer pageSize);
}
