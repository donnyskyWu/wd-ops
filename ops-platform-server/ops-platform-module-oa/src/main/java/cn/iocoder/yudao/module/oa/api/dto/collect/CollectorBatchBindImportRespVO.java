package cn.iocoder.yudao.module.oa.api.dto.collect;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CollectorBatchBindImportRespVO {

    private int scanned;
    private int imported;
    private int skipped;
    private int failed;
    private List<CollectorBatchBindImportItemVO> items = new ArrayList<>();

    @Data
    public static class CollectorBatchBindImportItemVO {
        private Long oaAccountId;
        private String platformType;
        private String result;
        private String message;
    }
}
