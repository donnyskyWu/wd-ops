package cn.iocoder.yudao.module.oa.api.dto.perf;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PerfTemplateVO {

    private Long id;
    private String position;
    private String positionLabel;
    private String templateName;
    private Integer isActive;
    private Integer itemCount;
    private LocalDateTime createTime;
}
