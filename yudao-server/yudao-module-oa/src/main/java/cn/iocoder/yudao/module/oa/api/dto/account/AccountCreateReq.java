package cn.iocoder.yudao.module.oa.api.dto.account;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AccountCreateReq {

    @NotBlank
    @InDict("dict_platform_type")
    private String platformType;

    @NotBlank
    @Size(max = 128)
    private String accountName;

    @NotBlank
    @Size(max = 64)
    private String externalAccountId;

    @InDict("dict_account_type")
    private String accountType;

    @NotNull
    private Long companyId;

    @NotNull
    private Long realnameId;

    private Long phoneId;

    private Long simCardId;

    private Long intermediaryId;

    private Long ipGroupId;

    private String cookie;

    @InDict("dict_account_status")
    private String status;

    private Boolean forceReplace;

    @Size(min = 5, max = 200)
    private String reason;
}
