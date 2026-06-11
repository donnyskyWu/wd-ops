package cn.iocoder.yudao.module.oa.framework.dingtalk;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oa.dingtalk")
public class DingTalkProperties {

    /** 是否启用钉钉集成 */
    private boolean enabled = false;

    /** 应用 AppKey / ClientId */
    private String clientId = "";

    /** 应用 AppSecret / ClientSecret */
    private String clientSecret = "";

    /** 企业 CorpId */
    private String corpId = "";

    /** 微应用 AgentId */
    private Long agentId;
}
