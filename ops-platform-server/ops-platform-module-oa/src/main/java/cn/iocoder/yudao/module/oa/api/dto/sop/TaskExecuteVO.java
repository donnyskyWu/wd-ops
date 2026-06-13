package cn.iocoder.yudao.module.oa.api.dto.sop;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
public class TaskExecuteVO {

    private Long id;
    private String planName;
    private String nodeName;
    private String nodeType;
    private Long ipGroupId;
    private String ipGroupName;
    private String competitionId;
    private String competitionName;
    private LocalDateTime slaDeadline;
    private String status;
    private Integer needReview;
    /** BLK-M2-008：SOP 节点 instruction_text，空则回退 nodeName */
    private String executionInstruction;
    /** SOP 节点 attachment_urls 参考附件（只读） */
    private List<TaskAttachmentVO> attachments = Collections.emptyList();
    /** 执行人上传的交付附件 */
    private List<TaskAttachmentVO> deliverableAttachments = Collections.emptyList();
    private String deliverables;
    private TaskLinkedContentVO linkedContent;
}
