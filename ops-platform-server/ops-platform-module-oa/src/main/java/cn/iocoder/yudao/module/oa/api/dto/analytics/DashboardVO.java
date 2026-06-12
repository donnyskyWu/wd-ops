package cn.iocoder.yudao.module.oa.api.dto.analytics;

import lombok.Data;

@Data
public class DashboardVO {

    private Long id;
    private String dashboardName;
    private String dashboardType;
    private String layout;
    private Integer status;
}
