package cn.iocoder.yudao.module.oa.api.dto.content;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LayoutStyleUpdateReq {

    @NotNull
    private Long id;
    private String name;
    private String category;
    private String tags;
    private String htmlSnippet;
    private Long thumbnailFileId;
    private Integer sort;
    private String status;
}
