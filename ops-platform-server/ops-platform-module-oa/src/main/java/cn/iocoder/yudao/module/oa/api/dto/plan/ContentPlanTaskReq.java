package cn.iocoder.yudao.module.oa.api.dto.plan;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentPlanTaskReq {

    @NotNull
    private Long nodeId;
    @NotBlank
    private String competitionId;
    @NotNull
    private Long assigneeId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledStart;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledEnd;

    /** Jackson 反序列化走 Lombok setter，字段上的 @JsonFormat 不生效 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setScheduledStart(LocalDateTime scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setScheduledEnd(LocalDateTime scheduledEnd) {
        this.scheduledEnd = scheduledEnd;
    }
}
