package cn.iocoder.yudao.module.oa.api.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AccountReplaceReq {

    private Long realnameId;

    private Long phoneId;

    private Long simCardId;

    @NotBlank
    @Size(min = 5, max = 200)
    private String reason;
}
