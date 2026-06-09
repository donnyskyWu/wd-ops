package cn.iocoder.yudao.module.oa.service.perf;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordAdjustReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordCalculateReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordConfirmReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordVO;

public interface PerfRecordService {

    PageResult<PerfRecordVO> list(Long ipGroupId, Long targetUserId, String periodType, String status,
                                  Integer pageNum, Integer pageSize);

    Long create(PerfRecordCreateReq req);

    void calculate(PerfRecordCalculateReq req);

    void adjust(PerfRecordAdjustReq req);

    PerfRecordDetailVO detail(Long id);

    void confirm(PerfRecordConfirmReq req);
}
