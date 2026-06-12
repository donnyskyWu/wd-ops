package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M6CustomQueryIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long createQuery(String name, String sql, String status) throws Exception {
        String body = String.format("""
                {"queryName":"%s","status":"%s","sqlText":"%s","params":null}
                """, name, status, sql);
        MvcResult r = mockMvc.perform(post("/admin-api/oa/query/create")
                        .header("Authorization", ADMIN).header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(0)).andReturn();
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(r.getResponse().getContentAsString()).get("data").asLong();
    }

    @Test
    @DisplayName("M6 CustomQuery: 列表")
    void list() throws Exception {
        long id = createQuery("IT-Q-list-" + System.nanoTime(),
                "SELECT id, query_name FROM oa_custom_query LIMIT 10", "DRAFT");
        mockMvc.perform(get("/admin-api/oa/query/list")
                        .header("Authorization", ADMIN).header("X-Tenant-Id", TENANT)
                        .param("pageNum", "1").param("pageSize", "20"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.list[?(@.id==" + id + ")]").exists());
        jdbc.update("DELETE FROM oa_custom_query WHERE id = ?", id);
    }

    @Test
    @DisplayName("M6 CustomQuery: 创建 DRAFT")
    void createDraft() throws Exception {
        long id = createQuery("IT-Q-D-" + System.nanoTime(),
                "SELECT 1 AS a, 'hello' AS b", "DRAFT");
        jdbc.update("DELETE FROM oa_custom_query WHERE id = ?", id);
    }

    @Test
    @DisplayName("M6 CustomQuery: 创建 PUBLISHED")
    void createPublished() throws Exception {
        long id = createQuery("IT-Q-P-" + System.nanoTime(),
                "SELECT 1 AS a", "PUBLISHED");
        jdbc.update("DELETE FROM oa_custom_query WHERE id = ?", id);
    }

    @Test
    @DisplayName("M6 CustomQuery: 非法 SQL 注入 → 2017")
    void rejectInjection() throws Exception {
        mockMvc.perform(post("/admin-api/oa/query/create")
                        .header("Authorization", ADMIN).header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"queryName":"IT-Q-注入","status":"DRAFT","sqlText":"DROP TABLE oa_custom_query","params":null}
                                """))
                .andExpect(jsonPath("$.code").value(2017));
    }

    @Test
    @DisplayName("M6 CustomQuery: 非法状态 → 1503")
    void rejectBadStatus() throws Exception {
        mockMvc.perform(post("/admin-api/oa/query/create")
                        .header("Authorization", ADMIN).header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"queryName":"IT-Q-非法状态","status":"BAD","sqlText":"SELECT 1","params":null}
                                """))
                .andExpect(jsonPath("$.code").value(1503));
    }

    @Test
    @DisplayName("M6 CustomQuery: execute 返回 rows")
    void execute() throws Exception {
        long id = createQuery("IT-Q-EX-" + System.nanoTime(),
                "SELECT 1 AS a, 'b' AS b", "DRAFT");
        mockMvc.perform(post("/admin-api/oa/query/" + id + "/execute")
                        .header("Authorization", ADMIN).header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.rows").exists());
        jdbc.update("DELETE FROM oa_custom_query WHERE id = ?", id);
    }

    @Test
    @DisplayName("M6 CustomQuery: publish 状态切换")
    void publish() throws Exception {
        long id = createQuery("IT-Q-PUB-" + System.nanoTime(),
                "SELECT 1", "DRAFT");
        mockMvc.perform(post("/admin-api/oa/query/" + id + "/publish")
                        .header("Authorization", ADMIN).header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));
        jdbc.update("DELETE FROM oa_custom_query WHERE id = ?", id);
    }

    @Test
    @DisplayName("M6 CustomQuery: 跨租户访问禁止 → 1504")
    void tenantForbidden() throws Exception {
        long id = createQuery("IT-Q-T-" + System.nanoTime(),
                "SELECT 1", "DRAFT");
        mockMvc.perform(post("/admin-api/oa/query/" + id + "/execute")
                        .header("Authorization", ADMIN).header("X-Tenant-Id", "999"))
                .andExpect(jsonPath("$.code").value(1504));
        jdbc.update("DELETE FROM oa_custom_query WHERE id = ?", id);
    }
}
