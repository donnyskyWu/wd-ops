package cn.iocoder.yudao.module.oa.api.dto.collect;

import lombok.Data;

@Data
public class CollectorQrLoginStartRespVO {

    private String sessionId;
    /** Base64 PNG；前端展示时补 `data:image/png;base64,` 前缀 */
    private String qrcodeBase64;
    private String status;
    private String message;
    /** 会话 TTL 秒数（来自 collector，默认约 300） */
    private Integer expiresInSeconds;
}
