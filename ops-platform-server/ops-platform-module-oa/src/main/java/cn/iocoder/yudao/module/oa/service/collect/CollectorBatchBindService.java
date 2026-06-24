package cn.iocoder.yudao.module.oa.service.collect;

import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorBatchBindImportRespVO;

public interface CollectorBatchBindService {

    /**
     * 租户内：凭证齐全且尚无 bind 行的 Channel-A 账号批量 import 至 collector。
     */
    CollectorBatchBindImportRespVO batchImport();
}
