package cn.iocoder.yudao.module.oa.api.dto.home;

import lombok.Data;

@Data
public class DashboardContentOverviewVO {
    private String date;
    private Long wechatCount;
    private Long douyinCount;
    private Long kuaishouCount;
    private Long xiaohongshuCount;
    private Long videoAccountCount;
}
