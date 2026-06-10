package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M6MetricCrudIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long createMetric(String name, String code, String type) throws Exception {
        String body = String.format("""
                {"metricName":"%s","metricCode":"%s","metricType":"%s","unit":"次"}
                """, name, code, type);
        MvcResult r = mockMvc.perform(post("/admin-api/oa/metric/create")
                        .header("Authorization", ADMIN).header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(0)).andReturn();
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(r.getResponse().getContentAsString()).get("data").asLong();
    }

    @Test
    @DisplayName("M6 Metric: 列表分页 + metricType 过滤")
    void listWithType() throws Exception {
        long id = createMetric("IT-M-列表-" + System.nanoTime(), "IT_M_LIST_" + System.nanoTime(), "BASIC");
        mockMvc.perform(get("/admin-api/oa/metric/list")
                        .header("Authorization", ADMIN).header("X-Tenant-Id", TENANT)
                        .param("pageNum", "1").param("pageSize", "20").param("metricType", "BASIC"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.list[?(@.id==" + id + ")].metricType").value("BASIC"));
    }

    @Test
    @DisplayName("M6 Metric: 创建 BASIC")
    void createBasic() throws Exception {
        long id = createMetric("IT-M-BASIC-" + System.nanoTime(), "IT_M_B_" + System.nanoTime(), "BASIC");
        if (id > 0) jdbc.update("DELETE FROM oa_metric WHERE id = ?", id);
    }

    @Test
    @DisplayName("M6 Metric: 创建 COMPOSITE")
    void createComposite() throws Exception {
        long id = createMetric("IT-M-COMPOSITE-" + System.nanoTime(), "IT_M_C_" + System.nanoTime(), "COMPOSITE");
        if (id > 0) jdbc.update("DELETE FROM oa_metric WHERE id = ?", id);
    }

    @Test
    @DisplayName("M6 Metric: 非法 type → 1503")
    void invalidType() throws Exception {
        mockMvc.perform(post("/admin-api/oa/metric/create")
                        .header("Authorization", ADMIN).header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metricName":"IT-M-非法","metricCode":"IT_M_INVALID","metricType":"BAD","unit":"次"}
                                """))
                .andExpect(jsonPath("$.code").value(1503));
    }

    @Test
    @DisplayName("M6 Metric: 更新 + status 切换")
    void updateAndStatus() throws Exception {
        long id = createMetric("IT-M-UPD-" + System.nanoTime(), "IT_M_U_" + System.nanoTime(), "BASIC");
        String body = String.format("""
                {"id":%d,"metricName":"IT-M-UPD-改","metricCode":"IT_M_U_%d","metricType":"BASIC","unit":"次","status":0}
                """, id, System.nanoTime());
        mockMvc.perform(put("/admin-api/oa/metric/update")
                        .header("Authorization", ADMIN).header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(0));
        jdbc.update("DELETE FROM oa_metric WHERE id = ?", id);
    }

    @Test
    @DisplayName("M6 Metric: 删除")
    void deleteMetricTest() throws Exception {
        long id = createMetric("IT-M-DEL-" + System.nanoTime(), "IT_M_D_" + System.nanoTime(), "BASIC");
        mockMvc.perform(delete("/admin-api/oa/metric/" + id)
                        .header("Authorization", ADMIN).header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M6 Metric: 重复 code → 2021")
    void duplicateCodeTest() throws Exception {
        String code = "IT_M_DUP_" + System.nanoTime();
        long id1 = createMetric("IT-M-DUP-1-" + System.nanoTime(), code, "BASIC");
        mockMvc.perform(post("/admin-api/oa/metric/create")
                        .header("Authorization", ADMIN).header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"metricName":"IT-M-DUP-2","metricCode":"%s","metricType":"BASIC","unit":"次"}
                                """, code)))
                .andExpect(jsonPath("$.code").value(2021));
        jdbc.update("DELETE FROM oa_metric WHERE id = ?", id1);
    }
}
