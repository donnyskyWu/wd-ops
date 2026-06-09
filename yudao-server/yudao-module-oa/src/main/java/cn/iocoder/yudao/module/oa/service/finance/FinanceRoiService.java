package cn.iocoder.yudao.module.oa.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.finance.FinanceRoiAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.finance.FinanceRoiBreakdownVO;
import cn.iocoder.yudao.module.oa.api.dto.finance.FinanceRoiTrendVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;

import java.time.LocalDate;

public interface FinanceRoiService {

    FinanceRoiAnalysisVO analysis(LocalDate startDate, LocalDate endDate, Long ipGroupId, Long accountId, String dimension);

    FinanceRoiTrendVO trend(LocalDate startDate, LocalDate endDate, Long ipGroupId, Long accountId);

    FinanceRoiBreakdownVO breakdown(LocalDate startDate, LocalDate endDate, Long ipGroupId, Long accountId);

    ExportJobVO export(LocalDate startDate, LocalDate endDate);
}
