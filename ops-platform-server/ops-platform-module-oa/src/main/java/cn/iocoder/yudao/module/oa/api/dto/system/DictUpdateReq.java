package cn.iocoder.yudao.module.oa.api.dto.system;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DictUpdateReq {

    @NotNull(message = "id 不能为空")
    private Long id;

    @NotBlank(message = "字典名称不能为空")
    private String dictName;

    /** ENABLED / DISABLED */
    private String status;

    @Valid
    private List<DictDataItemReq> items;
}
