package cn.iocoder.yudao.module.oa.api.dto.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImportResultVO {

    private int successCount;
    private int failCount;
    private List<String> failReasons = new ArrayList<>();
}
