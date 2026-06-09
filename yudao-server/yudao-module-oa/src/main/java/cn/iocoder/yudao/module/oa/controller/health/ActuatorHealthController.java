package cn.iocoder.yudao.module.oa.controller.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ActuatorHealthController {

    @GetMapping("/actuator/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
