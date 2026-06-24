package cn.iocoder.yudao.module.oa.api.dto.collect;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CollectLogResultVO {

    private String summary;
    private String dataType;
    private String targetTable;
    private String targetHint;
    private Integer recordCount;
    private Map<String, Object> metrics;
    private List<Map<String, Object>> samples;
    private List<Map<String, Object>> typeResults;
}
