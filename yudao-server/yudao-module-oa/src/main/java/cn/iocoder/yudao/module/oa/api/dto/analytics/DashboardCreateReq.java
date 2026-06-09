package cn.iocoder.yudao.module.oa.api.dto.analytics;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DashboardCreateReq {

    @NotBlank
    private String dashboardName;

    @NotBlank
    @InDict("dict_dashboard_type")
    private String dashboardType;

    private String layout;
}
