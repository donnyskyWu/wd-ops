package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContentPublishResultVO {

    private Long contentId;
    private String status;
    private boolean mock;
    /** 用户可读发布结果摘要（草稿 media_id、dev mock 提示等） */
    private String message;
    private List<RecordItem> records;

    @Data
    public static class RecordItem {
        private Long accountId;
        private String accountName;
        private String platformType;
        private String status;
        private String externalId;
        private String publishId;
        private String errorMessage;
        private LocalDateTime publishedAt;
    }
}
