package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M2PlanS09IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String LEADER = "Bearer dev-token-oa-leader";
    private static final String TENANT = "1";
    private static final String BASE = "/admin-api/oa/plan";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M2-S-09: 计划创建→启动→终止审批")
    void planLifecycle() throws Exception {
        MvcResult createResult = mockMvc.perform(post(BASE + "/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planName": "IT-6月内容计划",
                                  "templateId": 9401,
                                  "ipGroupId": 9001,
                                  "startDate": "2026-06-01",
                                  "endDate": "2026-06-30",
                                  "description": "集成测试计划",
                                  "competitions": [
                                    {"competitionId": "cmp-001", "competitionName": "SEED-春季赛事"}
                                  ],
                                  "steps": [
                                    {"nodeId": 9401, "competitionId": "cmp-001", "assigneeIds": [1003]},
                                    {"nodeId": 9402, "competitionId": "cmp-001", "assigneeIds": [1003]},
                                    {"nodeId": 9403, "competitionId": "cmp-001", "assigneeIds": [1003]}
                                  ]
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long planId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get(BASE + "/get")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", planId.toString()))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.competitions[0].competitionId").value("cmp-001"));

        mockMvc.perform(get("/admin-api/oa/task/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNum", "1")
                        .param("pageSize", "100"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.planName == 'IT-6月内容计划')]").isEmpty());

        mockMvc.perform(post(BASE + "/" + planId + "/start")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/task/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNum", "1")
                        .param("pageSize", "100"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.planName == 'IT-6月内容计划')]", hasSize(3)));

        mockMvc.perform(post(BASE + "/" + planId + "/terminate")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"测试终止\"}"))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post(BASE + "/" + planId + "/terminate/approve")
                        .header("Authorization", LEADER)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(delete(BASE + "/delete")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", planId.toString()))
                .andExpect(jsonPath("$.code").value(2023));
    }
}
