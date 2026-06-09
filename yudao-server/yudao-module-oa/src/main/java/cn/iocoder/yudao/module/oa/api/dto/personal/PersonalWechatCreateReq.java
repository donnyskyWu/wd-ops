package cn.iocoder.yudao.module.oa.api.dto.personal;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PersonalWechatCreateReq {

    @NotBlank
    private String accountName;
    @NotBlank
    private String wechatId;
    private Long phoneId;
    private String status;
}
