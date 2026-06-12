package cn.iocoder.yudao.module.oa.api.dto.home;

import lombok.Data;

@Data
public class TrendPointVO {
    private String date;
    private Long count;
    private String platform;
    /** IP 小组 ID（groupBy=IP_GROUP 时） */
    private Long ipGroupId;
    /** IP 小组名称（groupBy=IP_GROUP 时） */
    private String ipGroupName;
}
