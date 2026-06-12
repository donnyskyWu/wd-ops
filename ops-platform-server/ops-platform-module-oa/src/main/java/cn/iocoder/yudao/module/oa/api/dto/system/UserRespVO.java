package cn.iocoder.yudao.module.oa.api.dto.system;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserRespVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phoneMasked;
    private String position;
    private Long ipGroupId;
    private Long deptId;
    private String deptName;
    private String dingUserId;
    private String status;
    private String remark;
    private List<Long> roleIds;
    private List<String> roleNames;
    private LocalDateTime createTime;
}
