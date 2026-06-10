package cn.iocoder.yudao.module.oa.api.dto.author;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Data
public class AuthorDashboardVO {

    private Long authorId;
    private String authorName;
    private String ipGroupName;
    private Long followerCount = 0L;
    private List<FollowerTrendPoint> followerTrend = Collections.emptyList();
    private ContentStats contentStats = new ContentStats();
    private LiveStats liveStats = new LiveStats();
    private OrderAttribution orderAttribution = new OrderAttribution();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FollowerTrendPoint {
        private LocalDate date;
        private Long followerCount;
    }

    @Data
    public static class ContentStats {
        private Integer totalCount = 0;
        private Integer hitCount = 0;
        private Long avgRead = 0L;
    }

    @Data
    public static class LiveStats {
        private Double totalHours = 0.0;
        private Integer totalSessions = 0;
    }

    @Data
    public static class OrderAttribution {
        private Double totalRevenue = 0.0;
        private Double roi = 0.0;
    }
}
