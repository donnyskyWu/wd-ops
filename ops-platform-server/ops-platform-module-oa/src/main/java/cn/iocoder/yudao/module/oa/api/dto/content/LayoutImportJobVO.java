package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

@Data
public class LayoutImportJobVO {

    private Long id;
    private String status;
    private String sourceType;
    private String sourceUrl;
    private Object previewLayoutJson;
    private Object previewLayoutSchema;
    private Object extractionReport;
    private String suggestedName;
    private String errorMessage;
}
