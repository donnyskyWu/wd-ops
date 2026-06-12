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
class M4IntermediaryS03IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S-03: 实名人关联 2 个中介人 (1:N)")
    void createTwoIntermediaries() throws Exception {
        Long realnameId = createRealname();

        createIntermediary(realnameId, "李中介", "13900001111", "INTERMEDIARY", "10.5");
        createIntermediary(realnameId, "王代理", "13900002222", "AGENCY", "8");

        mockMvc.perform(get("/admin-api/oa/realname/" + realnameId + "/intermediaries")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].intermediaryPhoneMasked").exists())
                .andExpect(jsonPath("$.data[?(@.intermediaryName == '李中介')]").exists())
                .andExpect(jsonPath("$.data[?(@.intermediaryName == '王代理')]").exists());
    }

    @Test
    @DisplayName("S-03: 更新中介人")
    void updateIntermediary() throws Exception {
        Long realnameId = createRealname();
        Long intermediaryId = createIntermediary(realnameId, "赵中介", "13900003333", "DIRECT", "5");

        mockMvc.perform(put("/admin-api/oa/realname/intermediary/update")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"id": %d, "intermediaryName": "赵中介-更新", "commissionRate": 15}
                                """, intermediaryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("S-03: 删除中介人")
    void deleteIntermediary() throws Exception {
        Long realnameId = createRealname();
        Long intermediaryId = createIntermediary(realnameId, "待删中介", "13900004444", "DIRECT", "3");

        mockMvc.perform(delete("/admin-api/oa/realname/intermediary/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(intermediaryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/realname/" + realnameId + "/intermediaries")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private Long createRealname() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/oa/realname/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "realName": "中介测试人",
                                  "idType": "ID_CARD",
                                  "idCard": "330101199005055432",
                                  "phone": "13800138888",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);
    }

    private Long createIntermediary(Long realnameId, String name, String phone,
                                    String relationType, String commissionRate) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/oa/realname/" + realnameId + "/intermediary")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "intermediaryName": "%s",
                                  "intermediaryPhone": "%s",
                                  "relationType": "%s",
                                  "commissionRate": %s,
                                  "remark": "测试中介"
                                }
                                """, name, phone, relationType, commissionRate)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);
    }
}
