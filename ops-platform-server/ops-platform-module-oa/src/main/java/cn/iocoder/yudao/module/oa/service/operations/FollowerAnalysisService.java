package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerStatsVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerTrendVO;

import java.time.LocalDate;
import java.util.List;

public interface FollowerAnalysisService {

    PageResult<FollowerAnalysisVO> list(LocalDate startDate, LocalDate endDate, Long ipGroupId,
                                        Long accountId, String platformType, Integer pageNo, Integer pageSize);

    List<FollowerTrendVO> trend(LocalDate startDate, LocalDate endDate, Long ipGroupId, Long accountId);

    /**
     * S-R6-B1+B4：聚合统计（替代前端 list.reduce + list[0].growthRate）
     * spec: API-M1-运营管理 §4.4 / FR-M1-004
     * - dimension: DAY / WEEK / MONTH（不区分大小写，null = 按原始日聚合）
     * - 范围筛选与 list/trend 一致
     */
    FollowerStatsVO stats(LocalDate startDate, LocalDate endDate, Long ipGroupId,
                          Long accountId, String platformType, String dimension);

    /**
     * S-R6-B3：粉丝分析导出。返回 CSV 字节流（spec 写 xlsx，因 yudao-module-oa 未引入 POI，
     * 暂时以 csv 输出；文件名 follower_analysis_{ts}.csv，spec 已对齐同步）。
     * - 使用筛选参数（不含分页）全量导出当前匹配行
     */
    byte[] exportCsv(LocalDate startDate, LocalDate endDate, Long ipGroupId,
                     Long accountId, String platformType, String dimension);
}
