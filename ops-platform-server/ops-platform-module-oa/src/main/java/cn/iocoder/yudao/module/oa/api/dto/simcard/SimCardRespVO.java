package cn.iocoder.yudao.module.oa.api.dto.simcard;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SimCardRespVO {

    private Long id;
    private Long phoneId;
    private String phoneCode;
    private String phoneModel;
    private String phoneNumberMasked;
    private String isPrimary;
    private String operator;
    private Long assignedUserId;
    private String assignedUserName;
    private String iccidMasked;
    private String packageName;
    private String status;
    private Integer totalLinkedAccounts;
    private Integer wechatMpCount;
    private Integer douyinCount;
    private Integer weworkCount;
    private LocalDateTime createTime;
}
