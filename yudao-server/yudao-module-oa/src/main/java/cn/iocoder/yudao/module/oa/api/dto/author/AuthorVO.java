package cn.iocoder.yudao.module.oa.api.dto.author;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuthorVO {

    private Long id;
    private String authorName;
    private Long ipGroupId;
    private String ipGroupName;
    private String authorType;
    private Long primaryAccountId;
    private String primaryAccountName;
    private Long userId;
    private String userName;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
