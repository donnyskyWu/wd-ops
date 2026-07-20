package cn.iocoder.yudao.module.oa.api.dto.aicontent;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import cn.iocoder.yudao.module.oa.framework.dict.InDictList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiContentContextDTO {

    @NotBlank
    private String matchName;
    @NotBlank
    private String authorName;
    @NotEmpty(message = "不能为空")
    @InDictList("dict_scheme_type")
    private List<String> schemeTypes;
    private String historyRecord;
    @InDict("dict_anchor_style")
    private String anchorStyle;
    private String productDescription;

    /** 兼容旧版单选 schemeType 字符串入参 */
    @JsonIgnore
    public void setSchemeType(String schemeType) {
        if (StrUtil.isBlank(schemeType) || schemeTypes != null && !schemeTypes.isEmpty()) {
            return;
        }
        schemeTypes = new ArrayList<>();
        schemeTypes.add(schemeType.trim());
    }
}
