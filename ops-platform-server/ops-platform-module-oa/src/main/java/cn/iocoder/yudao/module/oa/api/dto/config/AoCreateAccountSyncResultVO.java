package cn.iocoder.yudao.module.oa.api.dto.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AoCreateAccountSyncResultVO {

    private int successCount;
    private int skipCount;
    private int failCount;
    private List<String> failReasons = new ArrayList<>();
}
