package cn.iocoder.yudao.module.oa.api.dto.system;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ParamUpdateReq {

    @NotNull(message = "id 不能为空")
    private Long id;

    @NotBlank(message = "参数名称不能为空")
    private String paramName;

    @NotBlank(message = "参数键不能为空")
    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_.]*$", message = "参数键格式不合法")
    private String paramKey;

    @NotBlank(message = "参数值不能为空")
    private String paramValue;

    @NotBlank(message = "参数类型不能为空")
    @InDict("dict_param_type")
    private String paramType;

    @NotBlank(message = "参数分类不能为空")
    @InDict("dict_param_category")
    private String category;

    private String remark;
}
