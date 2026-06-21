package cn.iocoder.yudao.module.oa.service.config.aochuang;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class AochuangFriendPageResult {

    private List<AochuangFriendDTO> friends = Collections.emptyList();
    private String nextCursor;
    private boolean hasMore;

    public static AochuangFriendPageResult of(List<AochuangFriendDTO> friends, String nextCursor, boolean hasMore) {
        AochuangFriendPageResult result = new AochuangFriendPageResult();
        result.setFriends(friends);
        result.setNextCursor(nextCursor);
        result.setHasMore(hasMore);
        return result;
    }
}
