package cn.iocoder.yudao.module.oa.service.home;

import cn.iocoder.yudao.module.oa.api.dto.home.DashboardAccountOverviewVO;
import cn.iocoder.yudao.module.oa.api.dto.home.DashboardAlertItemVO;
import cn.iocoder.yudao.module.oa.api.dto.home.DashboardContentOverviewVO;
import cn.iocoder.yudao.module.oa.api.dto.home.DashboardHomeKpiVO;
import cn.iocoder.yudao.module.oa.api.dto.home.DashboardTodoItemVO;
import cn.iocoder.yudao.module.oa.api.dto.home.HomeMetricsVO;
import cn.iocoder.yudao.module.oa.api.dto.home.PlatformDistVO;
import cn.iocoder.yudao.module.oa.api.dto.home.QuickActionVO;
import cn.iocoder.yudao.module.oa.api.dto.home.TodoVO;
import cn.iocoder.yudao.module.oa.api.dto.home.TrendPointVO;

import java.time.LocalDate;
import java.util.List;

public interface HomeDashboardService {

    DashboardHomeKpiVO getKpi(Long ipGroupId, LocalDate startDate, LocalDate endDate);

    List<DashboardAccountOverviewVO> getAccountOverview(Long ipGroupId);

    List<DashboardContentOverviewVO> getContentOverview(Long ipGroupId);

    List<DashboardAlertItemVO> getAlertList(Long ipGroupId, Integer limit);

    List<DashboardTodoItemVO> getTodoList(Long ipGroupId);

    HomeMetricsVO getMetrics(Long ipGroupId, LocalDate startDate, LocalDate endDate);

    List<TrendPointVO> getTrend(Long ipGroupId, LocalDate startDate, LocalDate endDate,
                                String platformType, String type, String groupBy);

    List<PlatformDistVO> getPlatformDist(Long ipGroupId, LocalDate startDate, LocalDate endDate);

    List<TodoVO> getTodos(Long ipGroupId, Integer limit);

    List<QuickActionVO> getQuickActions();

    void refresh();
}
