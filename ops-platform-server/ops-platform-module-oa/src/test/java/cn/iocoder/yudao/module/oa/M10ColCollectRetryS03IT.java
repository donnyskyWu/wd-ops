package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "oa.collect.stub.force-fail=true",
        "oa.collect.retry.max-attempts=3",
        "oa.collect.retry.backoff-seconds=0,0,0"
})
class M10ColCollectRetryS03IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M10-COL-S-03: 失败 3 次指数退避后任务 FAILED")
    void retryExhaustedMarksTaskFailed() throws Exception {
        Long taskId = createTask("重试耗尽任务");

        mockMvc.perform(post("/admin-api/oa/collect/task/" + taskId + "/run")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        waitUntilTaskStatus(taskId, "FAILED", 10_000);

        MvcResult taskResult = mockMvc.perform(get("/admin-api/oa/collect/task/" + taskId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String body = taskResult.getResponse().getContentAsString();
        assertEquals("FAILED", JsonPath.read(body, "$.data.status"));
        assertTrue(((Number) JsonPath.read(body, "$.data.failCount")).intValue() >= 1);

        MvcResult logResult = mockMvc.perform(get("/admin-api/oa/collect/log/page")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("taskId", String.valueOf(taskId))
                        .param("pageSize", "20"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String logBody = logResult.getResponse().getContentAsString();
        int total = JsonPath.read(logBody, "$.data.total");
        assertEquals(4, total);

        List<Integer> retryCounts = JsonPath.read(logBody, "$.data.list[*].retryCount");
        assertTrue(retryCounts.contains(0));
        assertTrue(retryCounts.contains(1));
        assertTrue(retryCounts.contains(2));
        assertTrue(retryCounts.contains(3));

        List<String> statuses = JsonPath.read(logBody, "$.data.list[*].status");
        assertTrue(statuses.stream().allMatch("FAILED"::equals));
    }

    @Test
    @DisplayName("M10-COL-S-03: FAILED 手动重置后可再次执行")
    void manualResetFromFailed() throws Exception {
        Long taskId = createTask("手动重试任务");

        mockMvc.perform(post("/admin-api/oa/collect/task/" + taskId + "/run")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        waitUntilTaskStatus(taskId, "FAILED", 10_000);

        mockMvc.perform(put("/admin-api/oa/collect/task/" + taskId + "/status")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("status", "PENDING"))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/collect/task/" + taskId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    private Long createTask(String name) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "name": "%s",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9001,
                                  "method": "INTERNAL",
                                  "source": "WECHAT_MP_API",
                                  "frequency": "DAILY",
                                  "cron": "0 0 2 * * ?",
                                  "status": "PENDING"
                                }
                                """, name)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);
    }

    private void waitUntilTaskStatus(Long taskId, String expectedStatus, int maxWaitMs) throws Exception {
        long deadline = System.currentTimeMillis() + maxWaitMs;
        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mockMvc.perform(get("/admin-api/oa/collect/task/" + taskId)
                            .header("Authorization", AUTH)
                            .header("X-Tenant-Id", TENANT))
                    .andReturn();
            String status = JsonPath.read(result.getResponse().getContentAsString(), "$.data.status");
            if (expectedStatus.equals(status)) {
                return;
            }
            Thread.sleep(200);
        }
        fail("Timeout waiting for task status " + expectedStatus);
    }
}
