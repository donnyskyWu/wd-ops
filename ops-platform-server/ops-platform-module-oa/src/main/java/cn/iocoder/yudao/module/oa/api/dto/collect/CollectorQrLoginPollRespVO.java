package cn.iocoder.yudao.module.oa.api.dto.collect;

import lombok.Data;

@Data
public class CollectorQrLoginPollRespVO {

    /** pending / scanned / confirmed / expired / error / timeout */
    private String status;
    private String message;
    /** collector 侧 account_id（confirmed 时） */
    private String collectorAccountId;
    /** 是否已将 cookie/token 写入 oa_account */
    private boolean credentialsSaved;
    /** confirmed 且已写 bind 时为 BOUND */
    private String bindStatus;
    private String connStatus;
}
