package cn.iocoder.yudao.module.oa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "oa.match")
public class OaMatchProperties {

    /**
     * 外部赛事 API 基址，如 http://110.42.49.224:48088/app-api/match
     */
    private String apiBaseUrl = "http://110.42.49.224:48088/app-api/match";

    /**
     * 转发外部 API 时附加的请求头（如鉴权 Token）
     */
    private Map<String, String> headers = new HashMap<>();
}
