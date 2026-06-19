package cn.iocoder.yudao.module.oa.api.dto.perf;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PerfTemplateCreateReq {

    @NotEmpty
    private List<String> positions;
    @NotBlank
    @Size(max = 100)
    private String templateName;
    private Integer isActive;
    @NotEmpty
    @Valid
    private List<PerfTemplateItemReq> items;
}
