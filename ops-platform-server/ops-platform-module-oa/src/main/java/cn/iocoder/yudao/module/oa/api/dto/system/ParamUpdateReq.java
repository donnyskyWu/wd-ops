package cn.iocoder.yudao.module.oa.api.dto.system;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ParamUpdateReq {

    @NotNull
    private Long id;
    @NotBlank
    private String paramName;
    @NotBlank
    private String paramKey;
    @NotBlank
    private String paramValue;
    @NotBlank
    @InDict("dict_param_type")
    private String paramType;
    @NotBlank
    @InDict("dict_param_category")
    private String category;
    private String remark;
}
