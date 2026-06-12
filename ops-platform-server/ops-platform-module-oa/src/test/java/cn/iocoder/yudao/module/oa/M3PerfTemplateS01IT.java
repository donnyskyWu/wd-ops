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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M3PerfTemplateS01IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M3-S-01: 创建模板并激活")
    void createAndActivate() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/perf/template/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "position": "OPERATOR",
                                  "templateName": "IT-运营专员考核",
                                  "isActive": 1,
                                  "items": [
                                    {
                                      "metricId": 9501,
                                      "weight": 100.00,
                                      "calcRule": "AUTO",
                                      "scoreStandard": {
                                        "ranges": [
                                          {"min": 0, "max": 20, "score": 60, "grade": "C"},
                                          {"min": 20, "max": 40, "score": 75, "grade": "B"},
                                          {"min": 40, "max": 60, "score": 85, "grade": "A"},
                                          {"min": 60, "max": 9999, "score": 100, "grade": "S"}
                                        ]
                                      }
                                    }
                                  ]
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        Long templateId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(post("/admin-api/oa/perf/template/activate")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": %d}".formatted(templateId)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/perf/template/" + templateId + "/items")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].metricName").value("推文发布数"));
    }

    @Test
    @DisplayName("M3-S-01: 权重合计≠100 (3001)")
    void weightValidation() throws Exception {
        mockMvc.perform(post("/admin-api/oa/perf/template/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "position": "OPERATOR",
                                  "templateName": "IT-权重错误",
                                  "items": [
                                    {
                                      "metricId": 9501,
                                      "weight": 60.00,
                                      "calcRule": "AUTO",
                                      "scoreStandard": {
                                        "ranges": [
                                          {"min": 0, "max": 9999, "score": 80, "grade": "A"}
                                        ]
                                      }
                                    }
                                  ]
                                }
                                """))
                .andExpect(jsonPath("$.code").value(3001));
    }

    @Test
    @DisplayName("M3-S-01: SEED 模板≥2")
    void seedTemplates() throws Exception {
        mockMvc.perform(get("/admin-api/oa/perf/template/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(2)));
    }
}
