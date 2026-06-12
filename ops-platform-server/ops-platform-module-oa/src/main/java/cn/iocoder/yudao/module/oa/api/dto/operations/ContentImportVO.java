package cn.iocoder.yudao.module.oa.api.dto.operations;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ContentImportVO {

    private Long id;
    private Long contentId;
    private String contentTitle;
    private LocalDate statDate;
    private String importType;
    private Long readCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer forwardCount;
    private Integer followerChange;
    private Integer reviewStatus;
    private String remark;
    private Long submitterId;
    private String submitterName;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
}
