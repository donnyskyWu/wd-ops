package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M2SopS01IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M2-S-01: SOP 模板 CRUD")
    void sopTemplateCrud() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/sop/template/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "IT-SOP模板",
                                  "contentType": "SHORT_VIDEO",
                                  "platformType": "DOUYIN",
                                  "description": "集成测试",
                                  "status": 1
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        Long templateId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get("/admin-api/oa/sop/template/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("templateName", "IT-SOP模板"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].templateName").value("IT-SOP模板"));

        mockMvc.perform(put("/admin-api/oa/sop/template/update")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": %d,
                                  "description": "已更新"
                                }
                                """.formatted(templateId)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(delete("/admin-api/oa/sop/template/" + templateId)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M2-S-01: SOP 模板创建 platformType=ALL（API-M2 §1.1）")
    void sopTemplateCreateWithAllPlatform() throws Exception {
        mockMvc.perform(post("/admin-api/oa/sop/template/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "IT-SOP-ALL平台",
                                  "contentType": "ALL",
                                  "platformType": "ALL",
                                  "description": "适用全部平台"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M2-S-01: SOP 节点创建")
    void sopNodeCreate() throws Exception {
        MvcResult templateResult = mockMvc.perform(post("/admin-api/oa/sop/template/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "IT-节点模板",
                                  "contentType": "ARTICLE",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "status": 1
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long templateId = JsonPath.parse(templateResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(post("/admin-api/oa/sop/node/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId": %d,
                                  "nodeName": "IT-写稿",
                                  "nodeOrder": 1,
                                  "nodeType": "NORMAL",
                                  "executorRole": "EDITOR",
                                  "needReview": 0,
                                  "predecessors": [],
                                  "slaHours": 24
                                }
                                """.formatted(templateId)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/sop/node/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("templateId", String.valueOf(templateId)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].nodeName").value("IT-写稿"));
    }

    @Test
    @DisplayName("M2-S-01: DAG 环检测 (2001)")
    void validateDagCycle() throws Exception {
        mockMvc.perform(post("/admin-api/oa/sop/node/validate-dag")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId": 9401,
                                  "nodes": [
                                    {"id": 1, "predecessors": [2]},
                                    {"id": 2, "predecessors": [1]}
                                  ]
                                }
                                """))
                .andExpect(jsonPath("$.code").value(2001));
    }

    @Test
    @DisplayName("M2-S-01: DAG 无环校验通过")
    void validateDagOk() throws Exception {
        mockMvc.perform(post("/admin-api/oa/sop/node/validate-dag")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId": 9401,
                                  "nodes": [
                                    {"id": 1, "predecessors": []},
                                    {"id": 2, "predecessors": [1]},
                                    {"id": 3, "predecessors": [2]}
                                  ]
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.valid").value(true));
    }

    @Test
    @DisplayName("M2-S-01: SEED 模板≥2")
    void seedTemplates() throws Exception {
        mockMvc.perform(get("/admin-api/oa/sop/template/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(2)));
    }
}
