package cn.iocoder.yudao.module.oa.api.dto.account;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FanGroupUpdateReq {

    @NotNull
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String groupName;

    @NotNull
    @Min(0)
    private Integer memberCount;
}
