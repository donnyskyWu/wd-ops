package cn.iocoder.yudao.module.oa.api.dto.author;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuthorUpdateReq {

    @NotNull
    private Long id;
    private String authorName;
    private Long ipGroupId;
    @InDict("dict_author_type")
    private String authorType;
    private Long primaryAccountId;
    private Long userId;
    private Integer status;
    private String remark;
}
