package cn.iocoder.yudao.module.oa.config;

import cn.iocoder.yudao.framework.common.biz.system.logger.OperateLogCommonApi;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2MasterTokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2TokenMapper;
import cn.iocoder.yudao.module.oa.framework.feign.OaTenantFeignRequestInterceptor;
import cn.iocoder.yudao.module.oa.framework.operatelog.OaLogRecordServiceImpl;
import cn.iocoder.yudao.module.oa.service.support.FootballSystemUserValidator;
import com.mzt.logapi.service.ILogRecordService;
import com.mzt.logapi.starter.annotation.EnableLogRecord;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AL-05: enable mzt-log {@code @LogRecord} and Feign write path to Football system-server.
 */
@Configuration
@EnableFeignClients(clients = OperateLogCommonApi.class)
@EnableLogRecord(tenant = "")
public class OaOperateLogConfiguration {

    @Bean
    public OaTenantFeignRequestInterceptor oaTenantFeignRequestInterceptor() {
        return new OaTenantFeignRequestInterceptor();
    }

    @Bean
    @Primary
    public ILogRecordService oaLogRecordService(OperateLogCommonApi operateLogCommonApi,
                                                FootballSystemUserValidator footballSystemUserValidator,
                                                FootballOAuth2MasterTokenMapper footballOAuth2MasterTokenMapper,
                                                FootballOAuth2TokenMapper footballOAuth2TokenMapper) {
        return new OaLogRecordServiceImpl(operateLogCommonApi, footballSystemUserValidator,
                footballOAuth2MasterTokenMapper, footballOAuth2TokenMapper);
    }
}
