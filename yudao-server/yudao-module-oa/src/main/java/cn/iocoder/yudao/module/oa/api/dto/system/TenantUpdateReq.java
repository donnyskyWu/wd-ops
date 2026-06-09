package cn.iocoder.yudao.module.oa.api.dto.system;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantUpdateReq {

    @NotNull
    private Long id;

    @Size(max = 64)
    private String tenantName;

    @Size(max = 64)
    private String contactName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;

    @Email
    @Size(max = 128)
    private String contactEmail;

    private LocalDateTime expireTime;

    @Min(1)
    private Integer maxAccounts;

    @InDict("dict_tenant_status")
    private String status;

    @Size(max = 512)
    private String remark;
}
