package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentStatsVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ContentAnalysisService {

    // S-R8 B6: 接口契约统一 page/size（与 controller 对齐）
    PageResult<ContentAnalysisVO> list(LocalDate startDate, LocalDate endDate, Long ipGroupId,
                                       Long accountId, String platformType, Boolean isHit,
                                       Integer page, Integer size);

    ContentStatsVO stats(LocalDate startDate, LocalDate endDate, Long ipGroupId, Long accountId);

    /**
     * 按日期聚合作品数据趋势
     * @param contentId 可选：限定到某条作品（详情场景）；null 时按筛选条件聚合
     * @return key=日期字符串(YYYY-MM-DD)，value={readCount, likeCount, commentCount, forwardCount, count}
     */
    List<Map<String, Object>> trend(LocalDate startDate, LocalDate endDate, Long ipGroupId,
                                    Long accountId, Long contentId);
}
