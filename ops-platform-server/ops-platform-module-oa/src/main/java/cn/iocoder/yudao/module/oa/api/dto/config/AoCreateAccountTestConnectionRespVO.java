package cn.iocoder.yudao.module.oa.api.dto.config;

import lombok.Data;

@Data
public class AoCreateAccountTestConnectionRespVO {

    private boolean success;
    private int deviceCount;
    private String connStatus;
    private String message;
}
