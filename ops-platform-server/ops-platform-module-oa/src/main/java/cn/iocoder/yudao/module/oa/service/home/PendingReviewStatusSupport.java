package cn.iocoder.yudao.module.oa.service.home;

import java.util.List;

/**
 * 首页待办聚合复用的「待审核」业务状态（跨 M0/M2 模块）。
 */
public final class PendingReviewStatusSupport {

    public static final String STATUS_PENDING_FIRST_REVIEW = "PENDING_FIRST_REVIEW";
    public static final String STATUS_PENDING_SECOND_REVIEW = "PENDING_SECOND_REVIEW";
    public static final String STATUS_PENDING_FINAL_REVIEW = "PENDING_FINAL_REVIEW";

    public static final List<String> CONTENT_REVIEW_STATUSES = List.of(
            STATUS_PENDING_FIRST_REVIEW,
            STATUS_PENDING_SECOND_REVIEW,
            STATUS_PENDING_FINAL_REVIEW
    );

    private PendingReviewStatusSupport() {
    }

    /** 首页 todo-list 汇总项标题 */
    public static String todoListTitle(String status) {
        return switch (status) {
            case STATUS_PENDING_FIRST_REVIEW -> "内容一级待审";
            case STATUS_PENDING_SECOND_REVIEW -> "内容二级待审";
            case STATUS_PENDING_FINAL_REVIEW -> "内容终审待审";
            default -> "内容待审核";
        };
    }

    /** 首页 todos 明细项标题 */
    public static String todoItemTitle(String contentTitle, String status) {
        String title = contentTitle == null ? "" : contentTitle;
        return switch (status) {
            case STATUS_PENDING_FIRST_REVIEW -> "内容《" + title + "》一级待审";
            case STATUS_PENDING_SECOND_REVIEW -> "内容《" + title + "》二级待审";
            case STATUS_PENDING_FINAL_REVIEW -> "内容《" + title + "》终审待审";
            default -> "内容《" + title + "》待审核";
        };
    }

    /** 内容审核页 deep link（stage=FIRST|SECOND|FINAL） */
    public static String reviewActionUrl(String status) {
        String stage = reviewStageQuery(status);
        return stage == null ? "/ops/content/review" : "/ops/content/review?stage=" + stage;
    }

    public static String reviewStageQuery(String status) {
        return switch (status) {
            case STATUS_PENDING_FIRST_REVIEW -> "FIRST";
            case STATUS_PENDING_SECOND_REVIEW -> "SECOND";
            case STATUS_PENDING_FINAL_REVIEW -> "FINAL";
            default -> null;
        };
    }
}
