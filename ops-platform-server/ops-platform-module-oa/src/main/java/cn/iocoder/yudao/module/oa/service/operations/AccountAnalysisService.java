package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.AccountAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerAnalysisVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AccountAnalysisService {

    PageResult<AccountAnalysisVO> list(String platform, Long ipGroupId, String keyword,
                                       Integer pageNo, Integer pageSize);

    PageResult<AccountAnalysisVO> list(String platform, Long ipGroupId, String keyword,
                                       Integer pageNo, Integer pageSize, LocalDate statDate);

    /**
     * 账号粉丝详情（按日期倒序）
     */
    List<FollowerAnalysisVO> listAccountFollowers(Long accountId, LocalDate startDate, LocalDate endDate);

    /**
     * 账号作品详情（分页）
     */
    PageResult<ContentAnalysisVO> listAccountContents(Long accountId, Integer pageNo, Integer pageSize);

    List<Map<String, Object>> accountFollowerTrend(Long accountId, LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> accountContentTrend(Long accountId, LocalDate startDate, LocalDate endDate);
}
