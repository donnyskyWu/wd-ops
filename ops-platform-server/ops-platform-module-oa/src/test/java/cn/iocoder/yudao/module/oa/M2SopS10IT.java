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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * S-10: SOP 节点类型（AC-M2-001-6 / TC-M2-001-09）
 */
@AutoConfigureMockMvc
class M2SopS10IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M2-S-10: 节点类型持久化 CONTENT_GENERATION")
    void sopNodeTypePersisted() throws Exception {
        MvcResult templateResult = mockMvc.perform(post("/admin-api/oa/sop/template/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "IT-节点类型模板",
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
                                  "nodeName": "IT-内容生成节点",
                                  "nodeOrder": 1,
                                  "nodeType": "CONTENT_GENERATION",
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
                .andExpect(jsonPath("$.data[0].nodeType").value("CONTENT_GENERATION"));
    }

    @Test
    @DisplayName("M2-S-10: 非法 nodeType → 1503")
    void sopNodeTypeInvalidReturns1503() throws Exception {
        MvcResult templateResult = mockMvc.perform(post("/admin-api/oa/sop/template/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "IT-非法节点类型",
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
                                  "nodeName": "IT-非法类型",
                                  "nodeOrder": 1,
                                  "nodeType": "CONTENT_GENERATE",
                                  "executorRole": "EDITOR",
                                  "needReview": 0,
                                  "predecessors": []
                                }
                                """.formatted(templateId)))
                .andExpect(jsonPath("$.code").value(1503));
    }
}
