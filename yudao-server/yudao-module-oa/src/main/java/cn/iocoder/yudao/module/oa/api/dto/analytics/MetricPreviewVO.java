package cn.iocoder.yudao.module.oa.api.dto.analytics;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MetricPreviewVO {

    private List<Map<String, Object>> rows;
}
