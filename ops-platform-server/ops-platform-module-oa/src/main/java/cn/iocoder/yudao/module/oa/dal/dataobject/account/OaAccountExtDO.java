package cn.iocoder.yudao.module.oa.dal.dataobject.account;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 微信公众号 Ops 扩展（wd.oa_account_ext，ADR-050 §C）。
 */
@Data
@TableName("oa_account_ext")
public class OaAccountExtDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long mpAccountId;

    private String platformType;

    private Long companyId;

    private Long realnameId;

    private Long intermediaryId;

    private Long ipGroupId;

    private Long phoneId;

    private Long simCardId;

    private String cookieEncrypted;

    private String trademarkName;

    private String qualificationType;

    private String usageStatus;

    private Long adminUserId;

    private String syncStatus;

    private String syncError;

    private String creator;

    private LocalDateTime createTime;

    private String updater;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
