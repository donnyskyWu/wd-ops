package cn.iocoder.yudao.module.oa.api.dto.company;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompanyCreateReq {

    @NotBlank
    @Size(max = 100)
    private String companyName;

    @NotBlank
    @Size(max = 18)
    @Pattern(regexp = "^[0-9A-Z]{18}$", message = "统一社会信用代码格式不正确")
    private String creditCode;

    @Size(max = 40)
    private String industry;

    @Size(max = 200)
    private String address;

    @Size(max = 64)
    private String legalName;

    private String legalIdCard;

    @Min(0)
    private Integer mpCapacityStandard;

    @InDict("dict_company_status")
    private String status;
}
