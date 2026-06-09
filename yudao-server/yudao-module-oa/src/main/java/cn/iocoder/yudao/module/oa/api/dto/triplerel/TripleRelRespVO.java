package cn.iocoder.yudao.module.oa.api.dto.triplerel;

import lombok.Data;

@Data
public class TripleRelRespVO {

    private Long id;
    private Long wechatAccountId;
    private String wechatName;
    private Long videoAccountId;
    private String videoName;
    private Long weworkAccountId;
    private String weworkName;
    private String relationType;
    private Integer status;
    private String bindTime;
}
