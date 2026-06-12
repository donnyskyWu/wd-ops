package cn.iocoder.yudao.module.oa.api.dto.system;

import lombok.Data;

@Data
public class OperationLogVO {

    private Long id;
    private String userName;
    private String module;
    private String action;
    private String level;
    private String content;
    private String method;
    private String params;
    private String response;
    private String ip;
    private String status;
    private String createTime;
}
