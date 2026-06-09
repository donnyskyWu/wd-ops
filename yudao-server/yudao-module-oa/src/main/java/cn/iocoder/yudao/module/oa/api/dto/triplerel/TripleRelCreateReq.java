package cn.iocoder.yudao.module.oa.api.dto.triplerel;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class TripleRelCreateReq {

    private Long personalWechatId;
    private List<Long> videoAccountIds;
    private Long weworkAccountId;
    @NotNull
    private String relationType;
}
