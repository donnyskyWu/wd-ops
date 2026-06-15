package cn.iocoder.yudao.module.oa.api.dto.phone;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class PhoneRespVO {

    private Long id;
    private Long realnameId;
    private String realName;
    private String phoneNumberMasked;
    private String phoneCode;
    private String phoneModel;
    private String settingsScreenshotKey;
    private String settingsScreenshotUrl;
    private String frontImageKey;
    private String frontImageUrl;
    private String backImageKey;
    private String backImageUrl;
    private String purchaseBatch;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime purchaseTime;

    private String handlerName;
    private String deviceNumber;
    private String isAochuang;
    private String phoneType;
    private Long keeperId;
    private String keeperName;
    private String wechatBound;
    private String status;
    private Integer accountBoundCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
