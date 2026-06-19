package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LayoutStyleVO {

    private Long id;
    private String styleCode;
    private String name;
    private String category;
    private String tags;
    private String htmlSnippet;
    private Long thumbnailFileId;
    private Integer sort;
    private String status;
    private LocalDateTime updateTime;
}
