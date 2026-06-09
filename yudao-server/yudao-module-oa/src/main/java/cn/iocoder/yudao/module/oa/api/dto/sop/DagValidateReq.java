package cn.iocoder.yudao.module.oa.api.dto.sop;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DagValidateReq {

    @NotNull
    private Long templateId;
    @NotNull
    private List<DagNodeItem> nodes;

    @Data
    public static class DagNodeItem {
        private Long id;
        private List<Long> predecessors;
    }
}
