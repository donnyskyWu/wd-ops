package cn.iocoder.yudao.module.oa.api.dto.system;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DictDataItemReq {

    private Long id;

    @NotBlank(message = "字典标签不能为空")
    private String dictLabel;

    @NotBlank(message = "字典值不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "字典值必须全大写+下划线")
    private String dictValue;

    private Integer sort;

    /** ENABLED / DISABLED */
    private String status;

    private String colorType;
    private String remark;
}
