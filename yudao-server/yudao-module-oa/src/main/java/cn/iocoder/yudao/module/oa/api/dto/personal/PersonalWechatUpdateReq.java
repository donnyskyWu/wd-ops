package cn.iocoder.yudao.module.oa.api.dto.personal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PersonalWechatUpdateReq {

    @NotNull
    private Long id;
    private String accountName;
    private String wechatId;
    private Long phoneId;
    private String status;
}
