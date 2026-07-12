package cn.iocoder.yudao.module.oa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oa.ai-generate")
public class AiGenerateProperties {

    /**
     * 调用外部 LLM 的最小超时（秒）。与 Gateway response-timeout（300s）对齐；
     * 低于此值的模型配置 timeout 会被抬升，避免 Read timed out。
     */
    private int llmTimeoutSeconds = 300;
}
