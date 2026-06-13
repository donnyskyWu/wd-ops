package cn.iocoder.yudao.module.oa.api.dto.plan;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContentPlanStepReq {

    @NotNull
    private Long nodeId;
    /** 兼容单选；与 competitionIds 二选一，至少填一项 */
    private String competitionId;
    /** 步骤关联赛事（多选） */
    private List<String> competitionIds;
    @NotEmpty
    private List<Long> assigneeIds;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledStart;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledEnd;
}