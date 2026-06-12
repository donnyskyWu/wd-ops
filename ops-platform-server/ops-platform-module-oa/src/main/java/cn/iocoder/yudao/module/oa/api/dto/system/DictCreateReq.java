package cn.iocoder.yudao.module.oa.api.dto.system;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class DictCreateReq {

    @NotBlank(message = "字典 type 不能为空")
    @Pattern(regexp = "^dict_[a-z0-9_]+$", message = "dictType 命名须 dict_ 前缀+小写下划线")
    private String dictType;

    @NotBlank(message = "字典名称不能为空")
    private String dictName;

    @NotEmpty(message = "字典项不能为空")
    @Valid
    private List<DictDataItemReq> items;
}
