package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class IpGroupAccountBindReq {

    @NotEmpty
    private List<Long> accountIds;
    private String accountRole;
}
