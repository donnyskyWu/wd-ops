package cn.iocoder.yudao.module.oa.api.dto.personal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PersonalWechatApiConfigReq {

    @NotNull
    private Long id;
    private String apiUrl;
    private String appId;
    private String appSecret;
    private String token;
}
