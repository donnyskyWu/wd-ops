package cn.iocoder.yudao.module.oa.api.dto.collect;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class CollectLogDetailRespVO extends CollectLogRespVO {

    private String platformType;
    private Long accountId;
    private String accountName;
    private String source;
    private String dataType;
    private LocalDateTime endAt;
    private CollectLogResultVO result;
}
