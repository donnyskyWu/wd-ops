package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectTaskMapper;
import cn.iocoder.yudao.module.oa.service.collect.CollectCronScheduler;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M10ColCollectScheduleS02IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CollectCronScheduler collectCronScheduler;

    @Autowired
    private CollectTaskMapper collectTaskMapper;

    @Test
    @DisplayName("M10-COL-S-02: 创建任务写入 nextRunAt")
    void createTaskSetsNextRunAt() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "调度nextRunAt任务",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9001,
                                  "method": "INTERNAL",
                                  "source": "WECHAT_MP_API",
                                  "frequency": "DAILY",
                                  "cron": "0 0 2 * * ?",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long taskId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get("/admin-api/oa/collect/task/" + taskId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nextRunAt").exists());

        CollectTaskDO task = collectTaskMapper.selectById(taskId);
        assertNotNull(task.getNextRunAt());
        assertTrue(task.getNextRunAt().isAfter(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    @DisplayName("M10-COL-S-02: cron 扫描 due 任务并写入日志")
    void cronScanRunsDueTask() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "调度扫描任务",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9001,
                                  "method": "INTERNAL",
                                  "source": "WECHAT_MP_API",
                                  "frequency": "DAILY",
                                  "cron": "0 0 2 * * ?",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long taskId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        CollectTaskDO task = collectTaskMapper.selectById(taskId);
        task.setNextRunAt(LocalDateTime.now().minusMinutes(1));
        collectTaskMapper.updateById(task);

        collectCronScheduler.scanTenantDueTasks(1L);

        mockMvc.perform(get("/admin-api/oa/collect/log/page")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("taskId", String.valueOf(taskId)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].status").value("SUCCESS"));

        CollectTaskDO afterRun = collectTaskMapper.selectById(taskId);
        assertNotNull(afterRun.getLastRunAt());
        assertNotNull(afterRun.getNextRunAt());
        assertTrue(afterRun.getNextRunAt().isAfter(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    @DisplayName("M10-COL-S-02: FAILED 任务不被 cron 扫描")
    void cronScanSkipsFailedTask() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "失败任务不调度",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9001,
                                  "method": "INTERNAL",
                                  "source": "WECHAT_MP_API",
                                  "frequency": "DAILY",
                                  "cron": "0 0 2 * * ?",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long taskId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        CollectTaskDO task = collectTaskMapper.selectById(taskId);
        task.setStatus("FAILED");
        task.setNextRunAt(LocalDateTime.now().minusMinutes(1));
        collectTaskMapper.updateById(task);

        collectCronScheduler.scanTenantDueTasks(1L);

        mockMvc.perform(get("/admin-api/oa/collect/log/page")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("taskId", String.valueOf(taskId)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
