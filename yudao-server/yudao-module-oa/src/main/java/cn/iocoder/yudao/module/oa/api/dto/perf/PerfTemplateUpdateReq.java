package cn.iocoder.yudao.module.oa.api.dto.perf;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PerfTemplateUpdateReq extends PerfTemplateCreateReq {

    @NotNull
    private Long id;
}
