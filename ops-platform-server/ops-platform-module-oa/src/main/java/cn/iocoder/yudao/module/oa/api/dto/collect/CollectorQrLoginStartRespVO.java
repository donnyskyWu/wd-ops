package cn.iocoder.yudao.module.oa.api.dto.collect;

import lombok.Data;

@Data
public class CollectorQrLoginStartRespVO {

    private String sessionId;
    /** Base64 PNG；前端展示时补 `data:image/png;base64,` 前缀 */
    private String qrcodeBase64;
    /** 部分平台（如视频号/B站）返回扫码 URL，前端直接作 img src */
    private String qrcodeUrl;
    private String status;
    private String message;
    /** 会话 TTL 秒数（来自 collector，默认约 300） */
    private Integer expiresInSeconds;
}
