package cn.iocoder.yudao.module.oa.api.dto.personal;

import lombok.Data;

@Data
public class WeworkEmployeeRespVO {

    private Long id;
    private Long weworkAccountId;
    private String nickname;
    private String weworkUserId;
    private String phone;
    private String department;
    private String position;
    private Long linkedPersonalWechatId;
    private String linkedPersonalWechatName;
    private String linkedWechatId;
    private String status;
    private String createTime;
}
