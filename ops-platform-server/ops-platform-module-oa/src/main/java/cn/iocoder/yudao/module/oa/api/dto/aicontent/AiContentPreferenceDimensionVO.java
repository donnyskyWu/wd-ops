package cn.iocoder.yudao.module.oa.api.dto.aicontent;

import lombok.Data;

@Data
public class AiContentPreferenceDimensionVO {

    private String value;
    private Double confidence;
    private Integer sourceRound;
}
