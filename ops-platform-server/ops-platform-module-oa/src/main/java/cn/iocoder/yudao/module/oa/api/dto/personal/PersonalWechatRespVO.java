package cn.iocoder.yudao.module.oa.api.dto.personal;

import lombok.Data;

@Data
public class PersonalWechatRespVO {

    private Long id;
    private String accountName;
    private String wechatId;
    private String contactPhone;
    private Long phoneId;
    private String phoneNumberMasked;
    private Long linkedWeworkEmployeeId;
    private String linkedWeworkEmployeeName;
    private String linkedWeworkUserId;
    private String status;
    private String apiUrl;
    private String appId;
    private String appSecret;
    private String token;
    private String createTime;
}
