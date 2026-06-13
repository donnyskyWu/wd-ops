package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

@Data
public class ContentScriptRefVO {

    private Long contentId;
    private String title;
    private String body;
    private String documentType;
    private String competitionId;
}
