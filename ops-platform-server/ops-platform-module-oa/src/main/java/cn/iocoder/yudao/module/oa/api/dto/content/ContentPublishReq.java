package cn.iocoder.yudao.module.oa.api.dto.content;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ContentPublishReq {

    @NotBlank
    @InDict("dict_platform_type")
    private String platformType;

    @NotEmpty
    private List<Long> accountIds;
}
