package cn.iocoder.yudao.module.oa.api.dto.sop;

import lombok.Data;

import java.util.List;

@Data
public class SopNodeVO {

    private Long id;
    private Long templateId;
    private String nodeName;
    private Integer nodeOrder;
    private String nodeType;
    private String instructionText;
    private List<TaskAttachmentVO> attachmentUrls;
    private String executorRole;
    private Integer needReview;
    private String reviewerRole;
    private List<Long> predecessors;
    private String parallelGroup;
    private Integer slaHours;
}
