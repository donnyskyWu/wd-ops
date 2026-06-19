package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M8 Metadata: seed content 实体列表")
    void listSeedEntity() throws Exception {
        mockMvc.perform(get("/admin-api/oa/metadata/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].entityCode").value("content"));
    }

    @Test
    @DisplayName("M8 Metadata: GET entity/{code}/fields 集成读口")
    void fieldsByCode() throws Exception {
        mockMvc.perform(get("/admin-api/oa/metadata/entity/content/fields")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.fieldCode=='platform_type')].queryConditionType")
                        .value("PLATFORM_SELECT"));
    }

    @Test
    @DisplayName("M8 Metadata: 从未映射表创建 account 实体")
    void createFromUnmappedTable() throws Exception {
        mockMvc.perform(post("/admin-api/oa/metadata/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entityCode": "account",
                                  "entityName": "账号",
                                  "physicalTable": "oa_account",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/metadata/entity/account/fields")
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
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("M8 Metadata: DICT 无 dict_type → 1400")
    void updateFieldDictRequiresType() throws Exception {
        String detail = mockMvc.perform(get("/admin-api/oa/metadata/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("entityCode", "content"))
                .andReturn().getResponse().getContentAsString();

        com.jayway.jsonpath.JsonPath.read(detail, "$.data.list[0].id");
        Long entityId = com.jayway.jsonpath.JsonPath.read(detail, "$.data.list[0].id");
        Long fieldId = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/admin-api/oa/metadata/" + entityId)
                                .header("Authorization", ADMIN)
                                .header("X-Tenant-Id", TENANT))
                        .andReturn().getResponse().getContentAsString(),
                "$.data.fields[0].id");

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
