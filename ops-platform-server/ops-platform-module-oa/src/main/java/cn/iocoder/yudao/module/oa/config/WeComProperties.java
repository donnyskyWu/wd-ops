package cn.iocoder.yudao.module.oa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oa.wework")
public class WeComProperties {

    /** 企微 OpenAPI 根地址（IT 可指向 MockWebServer） */
    private String baseUrl = "https://qyapi.weixin.qq.com";

    /** 测试/联调：跳过 HTTP，返回固定 stub 数据 */
    private boolean stub = false;

    private int timeoutMs = 15_000;
}
