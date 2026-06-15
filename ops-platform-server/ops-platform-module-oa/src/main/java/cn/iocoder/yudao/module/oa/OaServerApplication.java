package cn.iocoder.yudao.module.oa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("cn.iocoder.yudao.module.oa.dal.mysql")
public class OaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OaServerApplication.class, args);
    }
}
