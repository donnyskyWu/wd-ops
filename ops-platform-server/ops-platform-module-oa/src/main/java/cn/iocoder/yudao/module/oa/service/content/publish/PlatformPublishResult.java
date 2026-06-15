package cn.iocoder.yudao.module.oa.service.content.publish;

import lombok.Data;

@Data
public class PlatformPublishResult {

    private boolean success;
    private String externalId;
    private String errorMessage;
    private boolean mock;

    public static PlatformPublishResult success(String externalId, boolean mock) {
        PlatformPublishResult r = new PlatformPublishResult();
        r.setSuccess(true);
        r.setExternalId(externalId);
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
