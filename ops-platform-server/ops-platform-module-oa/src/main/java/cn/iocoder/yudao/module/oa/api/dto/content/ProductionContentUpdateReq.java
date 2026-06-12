package cn.iocoder.yudao.module.oa.api.dto.content;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductionContentUpdateReq {

    @NotNull
    private Long id;
    @Size(max = 200)
    private String title;
    @InDict("dict_content_type")
    private String contentType;
    @InDict("dict_platform_type")
    private String platformType;
    private Long accountId;
    private Long creatorUserId;
    private String body;
    private String coverImage;
    private Integer aiGenerated;
}
