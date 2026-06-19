package cn.iocoder.yudao.module.oa.api.dto.perf;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PerfTemplateVO {

    private Long id;
    private List<String> positions;
    /** 岗位标签列表，与 positions 一一对应 */
    private List<String> positionLabels;
    /** 岗位标签拼接，便于列表展示 */
    private String positionLabel;
    private String templateName;
    private Integer isActive;
    private Integer itemCount;
    private LocalDateTime createTime;
}
