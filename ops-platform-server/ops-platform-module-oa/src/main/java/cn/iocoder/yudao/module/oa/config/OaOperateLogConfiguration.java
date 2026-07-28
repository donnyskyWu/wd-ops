package cn.iocoder.yudao.module.oa.config;

import cn.iocoder.yudao.framework.common.biz.infra.file.FileApi;
import cn.iocoder.yudao.framework.common.biz.member.article.ArticleApi;
import cn.iocoder.yudao.framework.common.biz.mp.user.MpAccountInfoApi;
import cn.iocoder.yudao.framework.common.biz.pay.order.PayOrderApi;
import cn.iocoder.yudao.framework.common.biz.system.dict.DictDataApi;
import cn.iocoder.yudao.framework.common.biz.system.logger.OperateLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.user.AdminUserApi;
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
@EnableFeignClients(clients = {
        OperateLogCommonApi.class,
        AdminUserApi.class,
        PermissionCommonApi.class,
        DictDataApi.class,
        PayOrderApi.class,
        ArticleApi.class,
        MpAccountInfoApi.class,
        FileApi.class
})
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
