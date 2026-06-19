package cn.iocoder.yudao.module.oa.api.dto.perf;

import lombok.Data;

import java.util.List;

@Data
public class PerfTemplateItemsVO {

    private Long id;
    private String templateName;
    private List<String> positions;
    private List<String> positionLabels;
    private Integer isActive;
    private String remark;
    private List<PerfTemplateItemVO> items;
}
