package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class M8ConfigS01IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M8-S-01: 内部采集配置创建 + 列表")
    void internalCollectCreateAndList() throws Exception {
        createInternalCollect("M8-内部采集A");

        mockMvc.perform(get("/admin-api/oa/config/internal-collect/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("configName", "M8-内部采集A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].accountId").value(9001));
    }

    @Test
    @DisplayName("M8-S-01: 非法采集频率 (1503)")
    void invalidCollectFrequencyFails() throws Exception {
        mockMvc.perform(post("/admin-api/oa/config/internal-collect/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "configName": "M8-非法频率",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9001,
                                  "collectFrequency": "INVALID_FREQ",
                                  "collectMethod": "INTERNAL"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1503));
    }

    @Test
    @DisplayName("M8-S-02: 外部采集配置创建")
    void externalCollectCreate() throws Exception {
        mockMvc.perform(post("/admin-api/oa/config/external-collect/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "configName": "M8-外部采集A",
                                  "platformType": "NEWRANK",
                                  "collectFrequency": "DAILY",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M8-S-04: 阈值配置创建 + 列表")
    void thresholdCreateAndList() throws Exception {
        mockMvc.perform(post("/admin-api/oa/config/threshold/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "thresholdCategory": "ALERT",
                                  "metricName": "爆款阈值",
                                  "metricType": "HIT_THRESHOLD",
                                  "platformType": "DOUYIN",
                                  "thresholdValue": 1000000,
                                  "compareOperator": "GTE",
                                  "alertLevel": "WARNING"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/config/threshold/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("metricName", "爆款"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("M8-S-05: AI 模型 API Key 加密脱敏")
    void aiModelApiKeyMasked() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/oa/config/ai-model/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelName": "M8-GPT4",
                                  "modelType": "GPT",
                                  "apiKey": "sk-test-secret-key",
                                  "maxTokens": 4096
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long id = JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get("/admin-api/oa/config/ai-model/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("modelName", "M8-GPT4"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].apiKeyMasked").value("sk-****"));

        mockMvc.perform(delete("/admin-api/oa/config/ai-model/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(id)))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M8-S-05: AI 提示词模板 CRUD")
    void aiPromptCrud() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/oa/config/ai-prompt/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "M8-标题优化",
                                  "scene": "TITLE_OPT",
                                  "promptContent": "请优化标题：{{title}}"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long id = JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(put("/admin-api/oa/config/ai-prompt/update")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"id": %d, "templateName": "M8-标题优化V2"}
                                """, id)))
                .andExpect(jsonPath("$.code").value(0));
    }

    private void createInternalCollect(String name) throws Exception {
        mockMvc.perform(post("/admin-api/oa/config/internal-collect/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "configName": "%s",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9001,
                                  "collectFrequency": "DAILY",
                                  "collectMethod": "INTERNAL"
                                }
                                """, name)))
                .andExpect(jsonPath("$.code").value(0));
    }
}
