package cn.iocoder.yudao.module.oa.api.dto.perf;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PerfRecordCreateReq {

    @NotNull
    private Long targetUserId;
    @NotNull
    @InDict("dict_perf_period")
    private String periodType;
    @NotNull
    private LocalDate periodStart;
    @NotNull
    private LocalDate periodEnd;
    /** 可选；指定时优先使用所选模板，否则按被考核人岗位自动匹配 */
    private Long templateId;
}
