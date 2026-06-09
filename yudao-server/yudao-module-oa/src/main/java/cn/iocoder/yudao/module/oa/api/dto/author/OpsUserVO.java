package cn.iocoder.yudao.module.oa.api.dto.author;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OpsUserVO {

    private Long opsUserId;
    private String opsUserName;
    private Long ipGroupId;
    private LocalDate startDate;
    private LocalDate endDate;
}
