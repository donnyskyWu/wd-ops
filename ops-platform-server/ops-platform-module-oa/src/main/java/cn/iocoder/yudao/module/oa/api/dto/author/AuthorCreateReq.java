package cn.iocoder.yudao.module.oa.api.dto.author;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuthorCreateReq {

    @NotBlank
    private String authorName;
    @NotNull
    private Long ipGroupId;
    @InDict("dict_author_type")
    private String authorType;
    private Long primaryAccountId;
    private Long userId;
    private Integer status;
    private String remark;
}
