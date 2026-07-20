package cn.iocoder.yudao.module.oa.api.dto.aicontent;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AiContentPreferenceSummaryVO {

    private String summaryText;
    private Map<String, AiContentPreferenceDimensionVO> dimensions;
    private String sourceSessionId;
    private LocalDateTime generatedAt;
    private Boolean isUpdatedByUser;
}
