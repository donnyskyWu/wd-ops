package cn.iocoder.yudao.module.oa.api.dto.personal;

import lombok.Data;

@Data
public class WeworkTestConnectionRespVO {

    private boolean success;
    private String connStatus;
    private String message;
}
