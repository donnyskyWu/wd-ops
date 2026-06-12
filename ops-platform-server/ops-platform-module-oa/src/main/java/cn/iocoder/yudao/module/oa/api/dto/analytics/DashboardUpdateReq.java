package cn.iocoder.yudao.module.oa.api.dto.analytics;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DashboardUpdateReq {

    @NotNull
    private Long id;

    private String dashboardName;

    @InDict("dict_dashboard_type")
    private String dashboardType;

    private String layout;

    private Integer status;
}
