package cn.iocoder.yudao.module.oa.service.config.aochuang;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class AochuangMessagePageResult {

    private List<AochuangMessageDTO> messages = Collections.emptyList();
    private String nextCursor;
    private boolean hasMore;

    public static AochuangMessagePageResult of(List<AochuangMessageDTO> messages, String nextCursor, boolean hasMore) {
        AochuangMessagePageResult result = new AochuangMessagePageResult();
        result.setMessages(messages);
        result.setNextCursor(nextCursor);
        result.setHasMore(hasMore);
        return result;
    }
}
