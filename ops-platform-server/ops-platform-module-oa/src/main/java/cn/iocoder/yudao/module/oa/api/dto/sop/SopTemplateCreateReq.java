package cn.iocoder.yudao.module.oa.api.dto.sop;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SopTemplateCreateReq {

    @NotBlank
    @Size(max = 100)
    private String templateName;
    @InDict("dict_content_type")
    private String contentType;
    @InDict("dict_platform_type")
    private String platformType;
    @Size(max = 500)
    private String description;
    private Integer status;
}
