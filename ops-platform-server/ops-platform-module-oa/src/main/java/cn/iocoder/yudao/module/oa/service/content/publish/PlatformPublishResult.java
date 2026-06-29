package cn.iocoder.yudao.module.oa.service.content.publish;

import lombok.Data;

@Data
public class PlatformPublishResult {

    private boolean success;
    private String externalId;
    /** 正式发布返回的 publish_id（与 externalId 草稿 media_id 区分） */
    private String publishId;
    private String errorMessage;
    private boolean mock;

    public static PlatformPublishResult success(String externalId, boolean mock) {
        PlatformPublishResult r = new PlatformPublishResult();
        r.setSuccess(true);
        r.setExternalId(externalId);
        r.setMock(mock);
        return r;
    }

    public static PlatformPublishResult formalSuccess(String publishId, boolean mock) {
        PlatformPublishResult r = new PlatformPublishResult();
        r.setSuccess(true);
        r.setPublishId(publishId);
        r.setMock(mock);
        return r;
    }

    public static PlatformPublishResult failure(String message) {
        PlatformPublishResult r = new PlatformPublishResult();
        r.setSuccess(false);
        r.setErrorMessage(message);
        return r;
    }
}
