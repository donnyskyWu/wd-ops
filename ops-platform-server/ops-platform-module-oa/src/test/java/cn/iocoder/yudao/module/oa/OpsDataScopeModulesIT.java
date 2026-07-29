package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.plan.ContentPlanDO;
import cn.iocoder.yudao.module.oa.dal.mysql.plan.ContentPlanMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 多模块数据权限：Group A 仅本人/管理员；Group C 指标分析功能权限。
 */
@AutoConfigureMockMvc
class OpsDataScopeModulesIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String LEADER = "Bearer dev-token-oa-leader";
    private static final String OPERATOR = "Bearer dev-token-oa-operator";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContentPlanMapper contentPlanMapper;

    @Test
    @DisplayName("计划管理：运营组长不可见他人创建的计划")
    void leaderSeesOnlyOwnPlans() throws Exception {
        ContentPlanDO foreign = new ContentPlanDO();
        foreign.setTenantId(1L);
        foreign.setPlanName("IT-SCOPE-PLAN-FOREIGN");
        foreign.setTemplateId(9401L);
        foreign.setIpGroupId(9001L);
        foreign.setStartDate(LocalDate.now());
        foreign.setEndDate(LocalDate.now().plusDays(7));
        foreign.setStatus("DRAFT");
        foreign.setCreator("oa-admin");
        foreign.setUpdater("oa-admin");
        foreign.setCreateTime(LocalDateTime.now());
        foreign.setUpdateTime(LocalDateTime.now());
        contentPlanMapper.insert(foreign);

        mockMvc.perform(get("/admin-api/oa/plan/list")
                        .header("Authorization", LEADER)
                        .header("X-Tenant-Id", TENANT)
                        .param("planName", "IT-SCOPE-PLAN-FOREIGN"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/admin-api/oa/plan/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("planName", "IT-SCOPE-PLAN-FOREIGN"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));

        contentPlanMapper.deleteById(foreign.getId());
    }

    @Test
    @DisplayName("指标分析：运营专员可 list/preview（oa:metric-analysis:list）")
    void operatorCanUseMetricAnalysisApis() throws Exception {
        mockMvc.perform(get("/admin-api/oa/metric/list")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/admin-api/oa/metric/preview")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .contentType("application/json")
                        .content("{\"metricFormula\":\"SELECT 1 AS v\"}"))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("绩效执行：运营专员不可见他人发起的考核记录")
    void operatorSeesOnlyOwnPerfRecords() throws Exception {
        mockMvc.perform(get("/admin-api/oa/perf/record/list")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.targetUserId == 1002)]").doesNotExist());
    }
}
