package cn.iocoder.yudao.module.oa.api.dto.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AoCreateApiReq {

    @NotBlank
    @Size(max = 255)
    private String apiUrl;

    @NotBlank
    @Size(max = 100)
    private String appId;

    @Size(max = 512)
    private String appSecret;

    @Size(max = 512)
    private String token;
}
