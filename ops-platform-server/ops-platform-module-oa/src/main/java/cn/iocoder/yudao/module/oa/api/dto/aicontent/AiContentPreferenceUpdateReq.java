package cn.iocoder.yudao.module.oa.api.dto.aicontent;

import lombok.Data;

import java.util.Map;

@Data
public class AiContentPreferenceUpdateReq {

    private String summaryText;
    private Map<String, AiContentPreferenceDimensionVO> dimensions;
}
