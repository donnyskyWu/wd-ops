package cn.iocoder.yudao.module.oa.api.dto.sop;

import lombok.Data;

import java.util.List;

@Data
public class TaskExecuteSaveReq {

    /** 普通节点交付说明等草稿字段 */
    private String deliverables;
    /** 执行人上传的交付附件 */
    private List<TaskAttachmentVO> deliverableAttachments;
}
