package cn.iocoder.yudao.module.oa.api.dto.file;

import lombok.Data;

@Data
public class FileUploadVO {

    private String name;
    /** Relative storage key (tenant-scoped). */
    private String key;
    /** View URL without auth query params; append token for browser img src. */
    private String url;
}
