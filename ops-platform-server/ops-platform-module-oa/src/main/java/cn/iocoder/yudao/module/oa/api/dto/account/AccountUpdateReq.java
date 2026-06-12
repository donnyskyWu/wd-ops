package cn.iocoder.yudao.module.oa.api.dto.account;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AccountUpdateReq {

    @NotNull
    private Long id;

    @Size(max = 128)
    private String accountName;

    @InDict("dict_account_type")
    private String accountType;

    private Long companyId;

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
