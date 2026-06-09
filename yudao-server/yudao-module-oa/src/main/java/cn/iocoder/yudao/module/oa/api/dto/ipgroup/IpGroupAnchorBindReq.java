package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class IpGroupAnchorBindReq {

    @NotEmpty
    private List<Long> anchorUserIds;
    private String anchorType;
}
