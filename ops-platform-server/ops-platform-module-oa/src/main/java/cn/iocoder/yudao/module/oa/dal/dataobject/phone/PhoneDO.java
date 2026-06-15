package cn.iocoder.yudao.module.oa.dal.dataobject.phone;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_phone")
public class PhoneDO extends TenantBaseDO {

    private Long realnameId;
    private String phoneNumberEncrypted;
    private String phoneNumberHash;
    private String phoneCode;
    private String phoneModel;
    private String settingsScreenshotKey;
    private String frontImageKey;
    private String backImageKey;
    private String purchaseBatch;
    private LocalDate purchaseDate;
    private LocalTime purchaseTime;
    private String handlerName;
    private String deviceNumber;
    private String isAochuang;
    private String phoneType;
    private Long keeperId;
    private String wechatBound;
    private String status;
    private Integer accountBoundCount;
}
