package cn.iocoder.yudao.module.oa.api.dto.analytics;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class MetricPreviewReq {

    @NotBlank
    private String metricFormula;

    /** 指标参数化条件占位符绑定，如 p_account_id → 42 */
    private Map<String, String> bindParams;
}
