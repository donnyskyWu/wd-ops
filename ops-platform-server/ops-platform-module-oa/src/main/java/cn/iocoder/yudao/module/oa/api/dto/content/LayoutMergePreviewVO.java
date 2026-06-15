package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

@Data
public class LayoutMergePreviewVO {

    private Object layoutJson;
    private String layoutHtml;
    private Integer overflowSegmentCount;
}
