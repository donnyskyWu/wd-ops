package cn.iocoder.yudao.module.oa.api.dto.analytics;

import lombok.Data;

import java.util.List;

@Data
public class DashboardDataVO {
    private DashboardVO dashboard;
    private List<DashboardWidgetResultVO> widgets;
}
