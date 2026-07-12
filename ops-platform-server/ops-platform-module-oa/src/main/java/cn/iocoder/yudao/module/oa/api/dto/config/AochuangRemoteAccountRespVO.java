package cn.iocoder.yudao.module.oa.api.dto.config;

import lombok.Data;

@Data
public class AochuangRemoteAccountRespVO {

    private String accountId;
    private String userName;
    private Integer accountType;
    private String status;
    private String department;
    private String updateDate;
    /** 是否已同步到本地 oa_aocreate_account */
    private Boolean synced;
    /** 本地记录 ID（已同步时） */
    private Long localId;
}
