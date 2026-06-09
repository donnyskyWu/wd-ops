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
class M4CompanyS01IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S-01: 创建公司 + 列表 + mp-stats")
    void createListAndMpStats() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/company/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "测试传媒有限公司",
                                  "creditCode": "91330100MA2H123456",
                                  "industry": "传媒",
                                  "legalName": "李四",
                                  "legalIdCard": "330101199001011234",
                                  "mpCapacityStandard": 50,
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn();

        Long companyId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get("/admin-api/oa/company/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("companyName", "测试传媒"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].mpRemaining").value(50));

        mockMvc.perform(get("/admin-api/oa/company/" + companyId + "/mp-stats")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.capacity").value(50))
                .andExpect(jsonPath("$.data.registered").value(0))
                .andExpect(jsonPath("$.data.remaining").value(50));
    }

    @Test
    @DisplayName("S-01: 扩容公众号容量")
    void expandCapacity() throws Exception {
        Long companyId = createCompany("91330100MA2H654321", "扩容测试公司");

        mockMvc.perform(post("/admin-api/oa/company/" + companyId + "/expand")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newCapacity": 100, "reason": "业务扩张需要"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/company/" + companyId + "/mp-stats")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.capacity").value(100));
    }

    @Test
    @DisplayName("S-01: 更新公司")
    void updateCompany() throws Exception {
        Long companyId = createCompany("91330100MA2H111111", "更新测试公司");

        mockMvc.perform(put("/admin-api/oa/company/update")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"id": %d, "companyName": "更新后公司名", "status": "ENABLED"}
                                """, companyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("S-01: 删除空公司")
    void deleteCompany() throws Exception {
        Long companyId = createCompany("91330100MA2H222222", "删除测试公司");

        mockMvc.perform(delete("/admin-api/oa/company/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(companyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private Long createCompany(String creditCode, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/oa/company/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "companyName": "%s",
                                  "creditCode": "%s",
                                  "mpCapacityStandard": 10,
                                  "status": "ENABLED"
                                }
                                """, name, creditCode)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);
    }
}
