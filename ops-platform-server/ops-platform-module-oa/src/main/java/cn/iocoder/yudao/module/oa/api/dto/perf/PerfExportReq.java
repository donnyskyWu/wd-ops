package cn.iocoder.yudao.module.oa.api.dto.perf;

import lombok.Data;

import java.util.List;

@Data
public class PerfExportReq {

    private List<Long> ids;
}
