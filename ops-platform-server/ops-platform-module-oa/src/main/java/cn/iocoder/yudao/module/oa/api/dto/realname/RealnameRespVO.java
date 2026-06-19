package cn.iocoder.yudao.module.oa.api.dto.realname;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RealnameRespVO {

    private Long id;
    private Long companyId;
    private String companyName;
    private String realName;
    private String idType;
    private String idCardMasked;
    private String phoneMasked;
    private String wechat;
    private String gender;
    private String status;
    private Integer accountBoundCount;
    private String idCardFrontKey;
    private String idCardFrontUrl;
    private String idCardBackKey;
    private String idCardBackUrl;
    private LocalDateTime createTime;
}
