package cn.iocoder.yudao.module.oa.api.dto.sop;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SopNodeUpdateReq {

    @NotNull
    private Long id;
    @Size(max = 50)
    private String nodeName;
    private Integer nodeOrder;
    @InDict("dict_position")
    private String executorRole;
    private Integer needReview;
    @InDict("dict_position")
    private String reviewerRole;
    private List<Long> predecessors;
    private String parallelGroup;
    private Integer slaHours;
}
