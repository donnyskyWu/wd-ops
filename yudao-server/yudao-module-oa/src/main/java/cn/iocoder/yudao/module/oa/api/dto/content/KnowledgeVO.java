package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeVO {

    private Long id;
    private String title;
    private String content;
    private String category;
    private String tags;
    private Integer isPublic;
    private Integer status;
    private LocalDateTime createTime;
}
