package cn.iocoder.yudao.module.oa.api.dto.home;

import lombok.Data;

@Data
public class DashboardTodoItemVO {
    private String type;
    private String title;
    private Long count;
    private String route;
}
