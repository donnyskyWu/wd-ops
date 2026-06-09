package cn.iocoder.yudao.module.oa.service.perf;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfExportReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfResultVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTrendVO;

import java.time.LocalDate;

public interface PerfResultService {

    PageResult<PerfResultVO> list(Long userId, String periodType, String grade, LocalDate startDate,
                                  Integer pageNum, Integer pageSize);

    PerfTrendVO trend(Long userId, Integer month);

    ExportJobVO export(PerfExportReq req);
}
