package cn.iocoder.yudao.module.oa.api.dto.home;

import lombok.Data;

@Data
public class DashboardAccountOverviewVO {
    private String platformType;
    private Long accountCount;
    private Long followerCount;
}
