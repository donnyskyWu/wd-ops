package cn.iocoder.yudao.module.oa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * OA运营数据平台服务端启动类
 * <p>
 * 负责启动OA模块的Spring Boot应用，启用定时任务调度功能，
 * 并扫描MyBatis Mapper接口。
 * </p>
 *
 * @author system
 */

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("cn.iocoder.yudao.module.oa.dal.mysql")
public class OaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OaServerApplication.class, args);
    }
}
