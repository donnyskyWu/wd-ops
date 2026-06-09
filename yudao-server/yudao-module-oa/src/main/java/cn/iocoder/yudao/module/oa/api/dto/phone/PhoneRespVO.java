package cn.iocoder.yudao.module.oa.api.dto.phone;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PhoneRespVO {

    private Long id;
    private Long realnameId;
    private String realName;
    private String phoneNumberMasked;
    private String phoneCode;
    private String phoneModel;
    private Long keeperId;
    private String keeperName;
    private String wechatBound;
    private String status;
    private Integer accountBoundCount;
    private LocalDateTime createTime;
}
