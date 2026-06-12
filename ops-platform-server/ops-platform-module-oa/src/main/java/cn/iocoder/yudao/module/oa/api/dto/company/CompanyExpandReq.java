package cn.iocoder.yudao.module.oa.api.dto.company;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompanyExpandReq {

    @NotNull
    @Min(1)
    private Integer newCapacity;

    @NotBlank
    @Size(min = 2, max = 200)
    private String reason;
}
