package cn.iocoder.yudao.module.oa.api.dto.home;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DashboardAlertItemVO {
    private Long alertId;
    private String alertLevel;
    private String alertContent;
    private LocalDateTime triggerTime;
}
