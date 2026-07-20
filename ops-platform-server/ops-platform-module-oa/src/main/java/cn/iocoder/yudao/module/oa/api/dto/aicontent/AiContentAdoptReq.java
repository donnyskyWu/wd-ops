package cn.iocoder.yudao.module.oa.api.dto.aicontent;



import cn.iocoder.yudao.module.oa.framework.dict.InDictList;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotNull;

import lombok.Data;



import java.util.List;



@Data

public class AiContentAdoptReq {



    @NotBlank

    private String sessionId;

    private Long contentId;

    @NotBlank

    private String content;

    @NotNull

    private Long modelId;

    @InDictList("dict_scheme_type")

    private List<String> schemeTypes;

    private Integer roundCount;

}

