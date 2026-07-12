package cn.iocoder.yudao.module.oa.dal.dataobject.author;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作者 Ops 扩展（ADR-051）。
 * <p>
 * SSOT 为 shenyu-member.author_user；本表 PK = author_user_id，存 IP 组、作者类型、主推公号等运营维度。
 * </p>
 */
@Data
@TableName("oa_author_ext")
public class OaAuthorExtDO {

    @TableId(value = "author_user_id", type = IdType.INPUT)
    private Long authorUserId;

    private Long tenantId;

    private Long ipGroupId;

    private String authorType;

    private Long primaryMpAccountId;

    private Integer status;

    private String remark;

    /** SYNCED / ERROR */
    private String syncStatus;

    private String syncError;

    private String creator;

    private LocalDateTime createTime;

    private String updater;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
