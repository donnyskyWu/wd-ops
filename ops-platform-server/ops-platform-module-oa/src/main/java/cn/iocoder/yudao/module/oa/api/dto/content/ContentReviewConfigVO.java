package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

@Data
public class ContentReviewConfigVO {

    private boolean level1Enabled;
    private boolean level2Enabled;
    private String level1Role;
    private String level2Role;
}
