package cn.iocoder.yudao.module.oa.api.dto.content;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LayoutImportUrlReq {

    @NotBlank
    @Size(max = 1024)
    private String sourceUrl;
    @Size(max = 100)
    private String templateName;
    @InDict("dict_document_type")
    private String documentType;
}
