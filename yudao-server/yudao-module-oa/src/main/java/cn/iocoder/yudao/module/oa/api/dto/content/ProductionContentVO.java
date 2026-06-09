package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductionContentVO {

    private Long id;
    private String title;
    private String body;
    private String coverImage;
    private Long creatorUserId;
    private String creatorUserName;
    private Long accountId;
    private String accountName;
    private String platformType;
    private String contentType;
    private String status;
    private Integer aiGenerated;
    private LocalDateTime createTime;
}
