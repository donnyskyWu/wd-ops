package cn.iocoder.yudao.module.oa.api.dto.home;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TodoVO {
    private String title;
    private String source;
    private OffsetDateTime time;
    private String actionUrl;
}
