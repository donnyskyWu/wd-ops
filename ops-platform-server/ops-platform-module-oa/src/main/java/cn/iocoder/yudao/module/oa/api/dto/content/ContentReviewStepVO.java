package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContentReviewStepVO {

    /** DRAFT | FIRST_REVIEW | SECOND_REVIEW | PUBLISHED */
    private String stepKey;
    private String label;
    /** WAITING | IN_PROGRESS | COMPLETED | REJECTED | SKIPPED */
    private String stepStatus;
    private LocalDateTime completedAt;
    /** 已完成步骤的实际审核人姓名 */
    private String reviewerName;
    /** 待审步骤的审核角色标签，如「IP组长」 */
    private String reviewerRole;
    /** 待审步骤的可审核用户姓名列表 */
    private List<String> reviewerUsers;
    /** 展示用：角色+用户，如「IP组长：张三、李四」 */
    private String reviewerDisplay;
    private String comment;
}
