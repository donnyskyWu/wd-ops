package cn.iocoder.yudao.module.oa.api.dto.config;

import lombok.Data;

@Data
public class AiModelStatsVO {

    private long total;
    private long enabled;
    private long connected;
    private long defaultCount;
}
