package cn.iocoder.yudao.module.oa.api.dto.operations;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ImportContentDataReq {

    @NotNull
    private Long contentId;
    @NotNull
    private LocalDate statDate;
    @NotNull
    @InDict("dict_content_import_type")
    private String importType;
    private Long readCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer forwardCount;
    private Integer followerChange;
    private String remark;
}
