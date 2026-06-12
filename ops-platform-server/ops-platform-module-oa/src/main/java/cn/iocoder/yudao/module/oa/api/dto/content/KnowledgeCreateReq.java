package cn.iocoder.yudao.module.oa.api.dto.content;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeCreateReq {

    @NotBlank
    @Size(max = 100)
    private String title;
    @InDict("dict_knowledge_category")
    private String category;
    private String content;
    private String tags;
    private Integer isPublic;
}
