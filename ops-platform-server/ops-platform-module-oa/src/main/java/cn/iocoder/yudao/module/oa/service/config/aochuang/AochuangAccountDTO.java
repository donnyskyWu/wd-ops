package cn.iocoder.yudao.module.oa.service.config.aochuang;

import lombok.Data;

/**
 * 奥创 OpenAPI {@code GET /api/v1/accounts} 账号条目（ADR-045）。
 */
@Data
public class AochuangAccountDTO {

    /** 奥创 accountId（path 参数） */
    private String accountId;
    private String userName;
    /** 10=主账号，11=子账号 */
    private Integer accountType;
    private String status;
    private String department;
    private String updateDate;
}
