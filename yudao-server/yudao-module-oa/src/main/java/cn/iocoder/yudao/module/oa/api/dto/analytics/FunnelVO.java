package cn.iocoder.yudao.module.oa.api.dto.analytics;

import lombok.Data;

@Data
public class FunnelVO {

    private Long id;
    private String funnelName;
    private String funnelType;
    private Integer status;
}
