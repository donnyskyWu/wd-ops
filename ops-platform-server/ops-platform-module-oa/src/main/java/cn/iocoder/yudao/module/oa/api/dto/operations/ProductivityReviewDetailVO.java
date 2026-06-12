package cn.iocoder.yudao.module.oa.api.dto.operations;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * S-R9 B3: 人效展开详情（4 Card）
 * - 任务列表
 * - 财务指标（账号/IP 组对比）
 * - 内容指标
 * - 趋势图
 */
@Data
public class ProductivityReviewDetailVO {

    private ProductivityReviewVO summary;

    /** 任务详情（按 status 拆分） */
    private List<Map<String, Object>> taskList;

    /** 财务对比（按 IP 组/账号维度） */
    private List<Map<String, Object>> financeByGroup;

    /** 内容指标（占位：ContentDO 无 user 关联 → 全 0）*/
    private Map<String, Object> contentMetrics;

    /** 趋势（按日营收/任务完成数） */
    private List<Map<String, Object>> trend;
}
