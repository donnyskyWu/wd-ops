package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Phase 2 数据权限：自己创建 / 执行人过滤。
 */
@AutoConfigureMockMvc
class OpsDataScopePhase2IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String OPERATOR = "Bearer dev-token-oa-operator";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("运营专员任务列表仅见本人执行的任务")
    void operatorTaskListScopedToSelf() throws Exception {
        mockMvc.perform(get("/admin-api/oa/task/list")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.assigneeId == 1003)]").exists())
                .andExpect(jsonPath("$.data.list[?(@.assigneeId == 1005)]").doesNotExist());
    }

    @Test
    @DisplayName("管理员任务列表可见多执行人")
    void adminTaskListSeesMultipleAssignees() throws Exception {
        mockMvc.perform(get("/admin-api/oa/task/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.assigneeId == 1003)]").exists())
                .andExpect(jsonPath("$.data.list[?(@.assigneeId == 1005)]").exists());
    }

    @Test
    @DisplayName("运营专员不可见他人创建的自定义查询")
    void operatorCustomQueryListSelfOnly() throws Exception {
        mockMvc.perform(get("/admin-api/oa/query/list")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.id == 9861)]").doesNotExist());
    }

    @Test
    @DisplayName("管理员可见 seed 自定义查询")
    void adminCustomQueryListSeesSeed() throws Exception {
        mockMvc.perform(get("/admin-api/oa/query/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.id == 9861)]").exists());
    }

    @Test
    @DisplayName("运营专员 execute 他人查询 → 403")
    void operatorCannotExecuteOthersQuery() throws Exception {
        mockMvc.perform(post("/admin-api/oa/query/9861/execute")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("运营专员不可见 seed 漏斗与指标")
    void operatorFunnelAndMetricListSelfOnly() throws Exception {
        mockMvc.perform(get("/admin-api/oa/funnel/list")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.id == 9801)]").doesNotExist());

        mockMvc.perform(get("/admin-api/oa/metric/list")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.id == 9610)]").doesNotExist());
    }

    @Test
    @DisplayName("管理员可见 seed 漏斗与指标")
    void adminFunnelAndMetricListSeesSeed() throws Exception {
        mockMvc.perform(get("/admin-api/oa/funnel/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/admin-api/oa/metric/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.id == 9610)]").exists());
    }
}
