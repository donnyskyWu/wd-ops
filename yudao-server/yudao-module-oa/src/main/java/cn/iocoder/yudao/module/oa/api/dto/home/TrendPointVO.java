package cn.iocoder.yudao.module.oa.api.dto.home;

import lombok.Data;

@Data
public class TrendPointVO {
    private String date;
    private Long count;
    private String platform;
}
