package cn.iocoder.yudao.module.oa.api.dto.operations;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OpsAnchorRelVO {

    private Long id;
    private Long opsUserId;
    private String opsUserName;
    private Long anchorUserId;
    private String anchorUserName;
    private Long ipGroupId;
    private String ipGroupName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createTime;
}
