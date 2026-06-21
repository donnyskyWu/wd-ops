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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class M10AoPersonalWechatS01IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final String BASE = "/admin-api/oa/internal/personal-account";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M10-AO-S-01: 个微详情含奥创绑定字段默认值")
    void personalWechatAochuangFieldsDefault() throws Exception {
        MvcResult createResult = mockMvc.perform(post(BASE + "/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountName": "奥创测试个微",
                                  "wechatId": "wx_ao_s01",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long id = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get(BASE + "/" + id)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.aochuangBindStatus").value("UNBOUND"))
                .andExpect(jsonPath("$.data.aochuangWechatAccountId").doesNotExist());
    }
}
