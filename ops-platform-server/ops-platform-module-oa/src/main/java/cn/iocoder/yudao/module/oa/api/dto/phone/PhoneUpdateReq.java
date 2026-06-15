package cn.iocoder.yudao.module.oa.api.dto.phone;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class PhoneUpdateReq {

    @NotNull
    private Long id;

    private Long realnameId;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phoneNumber;

    @Size(max = 32)
    private String phoneCode;

    @Size(max = 100)
    private String phoneModel;

    @Size(max = 512)
    private String settingsScreenshotKey;

    @Size(max = 512)
    private String frontImageKey;

    @Size(max = 512)
    private String backImageKey;

    @Size(max = 64)
    private String purchaseBatch;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime purchaseTime;

    @Size(max = 64)
    private String handlerName;

    @Size(max = 64)
    private String deviceNumber;

    @InDict("dict_yes_no")
    private String isAochuang;

    @InDict("dict_phone_type")
    private String phoneType;

    private Long keeperId;

    @Size(max = 64)
    private String wechatBound;

    @InDict("dict_phone_status")
    private String status;
}
