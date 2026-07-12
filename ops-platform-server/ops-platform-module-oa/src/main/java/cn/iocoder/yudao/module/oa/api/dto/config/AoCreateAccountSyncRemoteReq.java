package cn.iocoder.yudao.module.oa.api.dto.config;

import lombok.Data;

import java.util.List;

@Data
public class AoCreateAccountSyncRemoteReq {

    /** 指定奥创 accountId；空且 syncAll=true 时同步全部子账号 */
    private List<String> aochuangAccountIds;

    /** 为 true 时同步全部远程子账号（忽略 aochuangAccountIds） */
    private Boolean syncAll;

    /** 全量拉取游标，默认 1970-01-01 00:00:00 */
    private String lastUpdateTime;
}
