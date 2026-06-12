package cn.iocoder.yudao.module.oa.api.dto.analytics;

import lombok.Data;

import java.util.Map;

@Data
public class DashboardWidgetResultVO {
    private String id;
    /** KPI | STAT | CHART | LIST */
    private String type;
    private String title;
    private Map<String, Object> payload;
}
