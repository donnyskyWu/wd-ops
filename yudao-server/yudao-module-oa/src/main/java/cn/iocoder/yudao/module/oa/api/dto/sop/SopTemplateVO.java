package cn.iocoder.yudao.module.oa.api.dto.sop;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SopTemplateVO {

    private Long id;
    private String templateName;
    private String contentType;
    private String platformType;
    private String description;
    private Integer status;
    private Integer nodeCount;
    private LocalDateTime createTime;
}
