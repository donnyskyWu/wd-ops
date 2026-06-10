package cn.iocoder.yudao.module.oa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class M11DictS01IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("M11-S-01: dict_author_type 含 LIVE / SHORT_VIDEO / ARTICLE / BOTH / IMAGE_TEXT")
    void authorTypeFullCoverage() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/oa/dict/data")
                        .param("type", "dict_author_type")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        JsonNode list = data.get("list");
        assertThat(list.isArray()).isTrue();
        assertThat(list.size()).isGreaterThanOrEqualTo(5);

        boolean hasLive = false, hasShort = false, hasArticle = false, hasBoth = false, hasImageText = false;
        for (JsonNode item : list) {
            String value = item.get("value").asText();
            if ("LIVE".equals(value)) hasLive = true;
            if ("SHORT_VIDEO".equals(value)) hasShort = true;
            if ("ARTICLE".equals(value)) hasArticle = true;
            if ("BOTH".equals(value)) hasBoth = true;
            if ("IMAGE_TEXT".equals(value)) hasImageText = true;
        }
        assertThat(hasLive && hasShort && hasArticle && hasBoth && hasImageText)
                .as("dict_author_type 必须覆盖 LIVE/SHORT_VIDEO/ARTICLE/BOTH/IMAGE_TEXT")
                .isTrue();
    }

    @Test
    @DisplayName("M11-S-01: dict_user_status / dict_cost_type / dict_platform_type 至少 2 项")
    void otherDictsReturnData() throws Exception {
        for (String t : new String[]{"dict_user_status", "dict_cost_type", "dict_platform_type"}) {
            mockMvc.perform(get("/admin-api/oa/dict/data")
                            .param("type", t)
                            .header("Authorization", ADMIN)
                            .header("X-Tenant-Id", TENANT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.list.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        }
    }

    @Test
    @DisplayName("M11-S-01: 缺失 type 参数 → 1500")
    void missingTypeReturns1500() throws Exception {
        mockMvc.perform(get("/admin-api/oa/dict/data")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1500));
    }

    @Test
    @DisplayName("M11-S-01: 不存在的 type → 2020")
    void unknownTypeReturns2020() throws Exception {
        mockMvc.perform(get("/admin-api/oa/dict/data")
                        .param("type", "dict_not_exist_xxx")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2020));
    }

    @Test
    @DisplayName("M11-S-01: 无 token → HTTP 401")
    void noTokenReturns401() throws Exception {
        mockMvc.perform(get("/admin-api/oa/dict/data")
                        .param("type", "dict_author_type")
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("M11-S-01: 字典 type 列表 types 接口包含 dict_author_type")
    void typesListContainsAuthorType() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/oa/dict/types")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("list");
        boolean found = false;
        for (JsonNode item : list) {
            if ("dict_author_type".equals(item.get("type").asText())) {
                found = true;
                break;
            }
        }
        assertThat(found).as("/dict/types 必须含 dict_author_type").isTrue();
    }

    // S-R2-Phase2: 补 time_dimension 字典（spec: API-M1-运营管理 §4.2）
    @Test
    @DisplayName("S-R2-Phase2: dict_time_dimension 含 DAY / WEEK / MONTH")
    void timeDimensionFullCoverage() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/oa/dict/data")
                        .param("type", "dict_time_dimension")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("list");
        java.util.Set<String> values = new java.util.HashSet<>();
        for (JsonNode item : list) {
            values.add(item.get("value").asText());
        }
        assertThat(values).contains("DAY", "WEEK", "MONTH");
    }

    // S-R6-TODO4：补个微 WECHAT_PERSONAL（spec: PRD-M1-运营管理 §4.3 TAB-001）
    @Test
    @DisplayName("S-R6-TODO4: dict_platform_type 含 6 老平台 + WECHAT_PERSONAL")
    void platformTypeIncludesPersonalWechat() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/oa/dict/data")
                        .param("type", "dict_platform_type")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("list");
        java.util.Set<String> values = new java.util.HashSet<>();
        for (JsonNode item : list) {
            values.add(item.get("value").asText());
        }
        // 6 个老平台 + V30 新增个微
        assertThat(values).contains(
                "WECHAT_OFFICIAL", "DOUYIN", "WEWORK", "WECHAT_VIDEO", "KUAISHOU", "XIAOHONGSHU",
                "WECHAT_PERSONAL"
        );
    }
}
