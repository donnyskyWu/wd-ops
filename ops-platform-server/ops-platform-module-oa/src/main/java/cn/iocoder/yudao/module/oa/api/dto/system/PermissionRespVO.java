package cn.iocoder.yudao.module.oa.api.dto.system;

import lombok.Data;

@Data
public class PermissionRespVO {

    private Long id;
    private String code;
    private String name;
    private String module;
}
