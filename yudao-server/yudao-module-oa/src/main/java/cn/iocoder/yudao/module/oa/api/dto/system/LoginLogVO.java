package cn.iocoder.yudao.module.oa.api.dto.system;

import lombok.Data;

@Data
public class LoginLogVO {

    private Long id;
    private String userName;
    private String ip;
    private String userAgent;
    private String status;
    private String message;
    private String createTime;
}
