package cn.iocoder.yudao.module.oa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oa.aochuang")
public class AochuangProperties {

    /** 测试/联调 stub：不调用真实奥创 OpenAPI */
    private boolean stub = false;
}
