package cn.iocoder.yudao.module.oa.api.dto.company;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CompanyRespVO {

    private Long id;
    private String companyName;
    private String creditCode;
    private String industry;
    private String address;
    private String legalName;
    private String legalIdCardMasked;
    private Integer mpCapacityStandard;
    private Integer mpRegisteredCount;
    private Integer mpRemaining;
    private String status;
    private List<String> businessLicenseKeys;
    private List<String> businessLicenseUrls;
    private LocalDateTime createTime;
}
