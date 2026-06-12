package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IpGroupStatusReq {

    @NotNull
    private Integer status;
}
