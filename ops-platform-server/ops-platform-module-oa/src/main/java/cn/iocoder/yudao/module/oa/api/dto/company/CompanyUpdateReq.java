package cn.iocoder.yudao.module.oa.api.dto.company;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompanyUpdateReq {

    @NotNull
    private Long id;

    @Size(max = 100)
    private String companyName;

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
