package cn.iocoder.yudao.module.oa.service.analytics;

import cn.iocoder.yudao.module.oa.api.dto.analytics.DashboardDataVO;

import java.time.LocalDate;

public interface DashboardDataService {

    DashboardDataVO loadData(Long dashboardId, Long ipGroupId, LocalDate startDate, LocalDate endDate, String platformType);
}
