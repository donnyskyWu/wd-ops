package cn.iocoder.yudao.module.oa.api.dto.system;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleRespVO {

    private Long id;
    private String code;
    private String name;
    private String status;
    private String remark;
    private List<Long> permissionIds;
    private List<String> permissionCodes;
    private LocalDateTime createTime;
}
