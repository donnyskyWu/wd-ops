package cn.iocoder.yudao.module.oa.api.dto.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgeLikeReq {

    @NotNull
    private Long id;
    /** "like" 收藏 / "unlike" 取消收藏 */
    @NotBlank
    private String action;
}
