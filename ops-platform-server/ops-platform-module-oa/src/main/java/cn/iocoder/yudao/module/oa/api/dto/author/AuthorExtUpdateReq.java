package cn.iocoder.yudao.module.oa.api.dto.author;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import lombok.Data;

@Data
public class AuthorExtUpdateReq {

    /** @deprecated ADR-055：归属 SSOT 为 IP 组管理，写入将返回 1105 */
    private Long ipGroupId;
    @InDict("dict_author_type")
    private String authorType;
    private Long primaryAccountId;
    private Integer status;
    private String remark;
}
