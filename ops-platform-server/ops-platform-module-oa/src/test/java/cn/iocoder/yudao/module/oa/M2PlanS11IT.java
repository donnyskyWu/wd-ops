package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M2PlanS11IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final String BASE = "/admin-api/oa/plan";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M2-S-11: 步骤分配赛事→任务继承 competitionId")
    void stepCompetitionInheritedByTasks() throws Exception {
        MvcResult createResult = mockMvc.perform(post(BASE + "/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planName": "IT-S11-赛事继承",
                                  "templateId": 9401,
                                  "ipGroupId": 9001,
                                  "startDate": "2026-06-01",
                                  "endDate": "2026-06-30",
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
                .andExpect(jsonPath("$.data.steps[0].competitionId").value("cmp-001"))
                .andExpect(jsonPath("$.data.steps[0].competitionName").value("SEED-春季赛事"));

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
                .andExpect(jsonPath("$.data.list[?(@.planName == 'IT-S11-赛事继承')]", hasSize(3)))
                .andExpect(jsonPath("$.data.list[?(@.planName == 'IT-S11-赛事继承' && @.competitionId == 'cmp-001')]", hasSize(3)));
    }

    @Test
    @DisplayName("M2-S-11: 步骤赛事不在计划池→1500")
    void stepCompetitionNotInPoolRejected() throws Exception {
        mockMvc.perform(post(BASE + "/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planName": "IT-S11-非法赛事",
                                  "templateId": 9401,
                                  "ipGroupId": 9001,
                                  "startDate": "2026-06-01",
                                  "endDate": "2026-06-30",
                                  "competitions": [
                                    {"competitionId": "cmp-001", "competitionName": "SEED-春季赛事"}
                                  ],
                                  "steps": [
                                    {"nodeId": 9401, "competitionId": "cmp-999", "assigneeIds": [1003]},
                                    {"nodeId": 9402, "competitionId": "cmp-001", "assigneeIds": [1003]},
                                    {"nodeId": 9403, "competitionId": "cmp-001", "assigneeIds": [1003]}
                                  ]
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1500));
    }

    @Test
    @DisplayName("M2-S-11: 草稿计划 update 成功→赛事/步骤刷新")
    void draftPlanUpdateSuccess() throws Exception {
        MvcResult createResult = mockMvc.perform(post(BASE + "/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planName": "IT-S11-待更新",
                                  "templateId": 9401,
                                  "ipGroupId": 9001,
                                  "startDate": "2026-06-01",
                                  "endDate": "2026-06-30",
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

        mockMvc.perform(put(BASE + "/update")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": %d,
                                  "planName": "IT-S11-已更新",
                                  "startDate": "2026-06-05",
                                  "endDate": "2026-06-25",
                                  "competitions": [
                                    {"competitionId": "cmp-002", "competitionName": "SEED-夏季赛事"}
                                  ],
                                  "steps": [
                                    {"nodeId": 9401, "competitionId": "cmp-002", "assigneeIds": [1003]},
                                    {"nodeId": 9402, "competitionId": "cmp-002", "assigneeIds": [1003]},
                                    {"nodeId": 9403, "competitionId": "cmp-002", "assigneeIds": [1003]}
                                  ]
                                }
                                """.formatted(planId)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get(BASE + "/get")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", planId.toString()))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.planName").value("IT-S11-已更新"))
                .andExpect(jsonPath("$.data.startDate").value("2026-06-05"))
                .andExpect(jsonPath("$.data.competitions[0].competitionId").value("cmp-002"))
                .andExpect(jsonPath("$.data.steps[0].competitionId").value("cmp-002"));
    }

    @Test
    @DisplayName("M2-S-11: update 步骤赛事不在池→1500")
    void updateStepCompetitionNotInPoolRejected() throws Exception {
        MvcResult createResult = mockMvc.perform(post(BASE + "/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planName": "IT-S11-update非法",
                                  "templateId": 9401,
                                  "ipGroupId": 9001,
                                  "startDate": "2026-06-01",
                                  "endDate": "2026-06-30",
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

        mockMvc.perform(put(BASE + "/update")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": %d,
                                  "planName": "IT-S11-update非法",
                                  "startDate": "2026-06-01",
                                  "endDate": "2026-06-30",
                                  "competitions": [
                                    {"competitionId": "cmp-001", "competitionName": "SEED-春季赛事"}
                                  ],
                                  "steps": [
                                    {"nodeId": 9401, "competitionId": "cmp-999", "assigneeIds": [1003]},
                                    {"nodeId": 9402, "competitionId": "cmp-001", "assigneeIds": [1003]},
                                    {"nodeId": 9403, "competitionId": "cmp-001", "assigneeIds": [1003]}
                                  ]
                                }
                                """.formatted(planId)))
                .andExpect(jsonPath("$.code").value(1500));
    }
}
