package cn.iocoder.yudao.module.oa.dal.dataobject.author;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Football 作者 SSOT（shenyu-member.author_user，ADR-051）。
 */
@Data
@TableName("author_user")
public class AuthorUserDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String nickname;

    private String avatarUrl;

    private Integer status;

    /** 作者等级：0=作者 1=专家（member.author_user.author_level） */
    private Integer authorLevel;

    private Long tenantId;

    private String creator;

    private LocalDateTime createTime;

    private String updater;

    private LocalDateTime updateTime;
}
