package cn.iocoder.yudao.module.oa.api.dto.collect;

import lombok.Data;

@Data
public class CollectorAccountBindTestConnectionRespVO {

    private boolean success;
    private String connStatus;
    private String collectorAccountId;
    private String collectorStatus;
    private String message;
}
