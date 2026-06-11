package cn.iocoder.yudao.module.oa.api.dto.system;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeptUpdateReq {

    @NotNull
    private Long id;

    private Long parentId;

    @Size(max = 128)
    private String name;

    private Integer sort;

    @InDict("dict_user_status")
    private String status;
}
