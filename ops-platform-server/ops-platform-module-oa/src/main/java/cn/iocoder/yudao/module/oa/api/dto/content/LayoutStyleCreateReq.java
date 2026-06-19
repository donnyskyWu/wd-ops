package cn.iocoder.yudao.module.oa.api.dto.content;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LayoutStyleCreateReq {

    @NotBlank
    private String styleCode;
    @NotBlank
    private String name;
    @NotBlank
    private String category;
    private String tags;
    @NotBlank
    private String htmlSnippet;
    private Long thumbnailFileId;
    private Integer sort;
    private String status;
}
