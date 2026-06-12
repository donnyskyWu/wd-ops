package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.ProductivityReviewVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ProductivityReviewDetailVO;

import java.time.LocalDate;
import java.util.List;

public interface ProductivityReviewService {

    // S-R9 B1: 收 startDate/endDate/timeDimension/positionKeyword
    // S-R9 B9: 统一 page/size 契约
    PageResult<ProductivityReviewVO> list(LocalDate startDate, LocalDate endDate,
                                          String timeDimension,
                                          Long ipGroupId, Long userId, String position,
                                          String keyword,
                                          Integer page, Integer size);

    // S-R9 B3: 4 Card 展开详情（任务列表 + 财务对比 + 内容指标 + 趋势）
    ProductivityReviewDetailVO detail(Long userId, LocalDate startDate, LocalDate endDate);

    // S-R9 B3: 主播详情（按 IP 组下所有账号）
    List<ProductivityReviewVO> anchors(Long ipGroupId, LocalDate startDate, LocalDate endDate);

    // S-R9 B3: 导出 CSV
    String exportCsv(LocalDate startDate, LocalDate endDate, String timeDimension,
                     Long ipGroupId, Long userId, String position, String keyword);
}
