package cn.iocoder.yudao.module.oa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "oa.collect")
public class CollectProperties {

    private Stub stub = new Stub();
    private Retry retry = new Retry();
    private Schedule schedule = new Schedule();

    @Data
    public static class Stub {
        /** 测试/联调：强制采集失败以验证重试链路 */
        private boolean forceFail = false;
    }

    @Data
    public static class Retry {
        /** 失败后最大重试次数（指数退避 1/5/15min） */
        private int maxAttempts = 3;
        /** 各次重试等待秒数，默认 60/300/900 */
        private List<Integer> backoffSeconds = List.of(60, 300, 900);
    }

    @Data
    public static class Schedule {
        /** 是否启用 cron 调度扫描 */
        private boolean enabled = true;
        /** 扫描 due 任务的 cron（默认每分钟整点） */
        private String scanCron = "0 * * * * ?";
    }
}
