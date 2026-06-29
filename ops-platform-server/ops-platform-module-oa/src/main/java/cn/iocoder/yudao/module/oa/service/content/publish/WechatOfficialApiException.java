package cn.iocoder.yudao.module.oa.service.content.publish;

/**
 * 微信公众号 Open API 调用异常。
 */
public class WechatOfficialApiException extends RuntimeException {

    public WechatOfficialApiException(String message) {
        super(message);
    }
}
