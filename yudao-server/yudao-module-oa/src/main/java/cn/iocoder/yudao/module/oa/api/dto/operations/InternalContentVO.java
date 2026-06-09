package cn.iocoder.yudao.module.oa.api.dto.operations;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InternalContentVO {

    private Long id;
    private Long accountId;
    private String accountName;
    private String title;
    private String platformType;
    private String contentType;
    private LocalDateTime publishTime;
    private Long readCount;
    private Integer likeCount;
    private String dataSource;
    private Boolean isHit;
}
