package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class M8MetadataIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String LEADER = "Bearer dev-token-oa-leader";
    private static final String TENANT = "1";
    private static final String TEST_ENTITY_CODE = "it_wx_mp_follower";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanupTestMetadata() {
        jdbcTemplate.update("""
                DELETE FROM sys_metadata_field
                WHERE tenant_id = 1 AND entity_id IN (
                    SELECT id FROM sys_metadata_entity
                    WHERE tenant_id = 1 AND entity_code = ?
                )
                """, TEST_ENTITY_CODE);
        jdbcTemplate.update(
                "DELETE FROM sys_metadata_entity WHERE tenant_id = 1 AND entity_code = ?",
                TEST_ENTITY_CODE);
    }

    @Test
    @DisplayName("M8 Metadata: seed oa_content 实体可读")
    void listSeedEntity() throws Exception {
        Long entityId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_metadata_entity WHERE tenant_id = 1 AND entity_code = 'oa_content'",
                Long.class);

        mockMvc.perform(get("/admin-api/oa/metadata/" + entityId)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.entityCode").value("oa_content"))
                .andExpect(jsonPath("$.data.physicalTable").value("oa_content"));
    }

    @Test
    @DisplayName("M8 Metadata: GET entity/{code}/fields 集成读口")
    void fieldsByCode() throws Exception {
        mockMvc.perform(get("/admin-api/oa/metadata/entity/oa_content/fields")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.fieldCode=='platform_type')].queryConditionType")
                        .value("PLATFORM_SELECT"));
    }

    @Test
    @DisplayName("M8 Metadata: 从未映射表创建 wx_mp_follower 实体")
    void createFromUnmappedTable() throws Exception {
        mockMvc.perform(post("/admin-api/oa/metadata/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entityCode": "%s",
                                  "entityName": "公众号粉丝快照",
                                  "physicalTable": "oa_wechat_mp_follower",
                                  "status": "ENABLED"
                                }
                                """.formatted(TEST_ENTITY_CODE)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/metadata/entity/" + TEST_ENTITY_CODE + "/fields")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").isNumber());
    }

    @Test
    @DisplayName("M8 Metadata: 非超级管理员删除 → 403")
    void deleteForbiddenForNonSuperAdmin() throws Exception {
        mockMvc.perform(delete("/admin-api/oa/metadata/999999")
                        .header("Authorization", LEADER)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("M8 Metadata: DICT 无 dict_type → 1400")
    void updateFieldDictRequiresType() throws Exception {
        Long entityId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_metadata_entity WHERE tenant_id = 1 AND entity_code = 'oa_content'",
                Long.class);
        Number fieldIdNum = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/admin-api/oa/metadata/" + entityId)
                                .header("Authorization", ADMIN)
                                .header("X-Tenant-Id", TENANT))
                        .andReturn().getResponse().getContentAsString(),
                "$.data.fields[0].id");
        long fieldId = fieldIdNum.longValue();

        mockMvc.perform(put("/admin-api/oa/metadata/" + entityId + "/fields")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fields": [{
                                    "id": %d,
                                    "fieldName": "测试",
                                    "queryConditionType": "DICT",
                                    "sort": 10
                                  }]
                                }
                                """.formatted(fieldId)))
                .andExpect(jsonPath("$.code").value(1400));
    }
}
