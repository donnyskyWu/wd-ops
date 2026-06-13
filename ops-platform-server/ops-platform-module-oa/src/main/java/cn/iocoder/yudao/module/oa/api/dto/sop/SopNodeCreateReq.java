package cn.iocoder.yudao.module.oa.api.dto.sop;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SopNodeCreateReq {

    @NotNull
    private Long templateId;
    @NotBlank
    @Size(max = 50)
    private String nodeName;
    @NotNull
    private Integer nodeOrder;
    @NotBlank
    @InDict("dict_sop_node_type")
    private String nodeType;
    @Size(max = 2000)
    private String instructionText;
    @NotBlank
    @InDict("dict_position")
    private String executorRole;
    private Integer needReview;
    @InDict("dict_position")
    private String reviewerRole;
    private List<Long> predecessors;
    private String parallelGroup;
    private Integer slaHours;
}
