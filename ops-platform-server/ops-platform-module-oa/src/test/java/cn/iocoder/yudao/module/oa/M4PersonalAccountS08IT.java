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
class M4PersonalAccountS08IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final String BASE = "/admin-api/oa/internal/personal-account";
    private static final String WEWORK = "/admin-api/oa/internal/wework";
    private static final String WEWORK_EMP = "/admin-api/oa/internal/wework/employee";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S-08: 个微 CRUD + 奥创脱敏")
    void personalWechatCrudAndMask() throws Exception {
        String createBody = """
                {
                  "accountName": "测试个微",
                  "wechatId": "test_wx_s08",
                  "contactPhone": "13800138008",
                  "status": "ENABLED"
                }
                """;
        String idStr = mockMvc.perform(post(BASE + "/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        Number num = com.jayway.jsonpath.JsonPath.read(idStr, "$.data");
        Long id = num.longValue();

        mockMvc.perform(post(BASE + "/api-config")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "id": %d,
                                  "apiUrl": "https://api.aocang.com",
                                  "appId": "app123",
                                  "appSecret": "secret456",
                                  "token": "token789"
                                }
                                """, id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get(BASE + "/" + id)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.contactPhone").value("13800138008"))
                .andExpect(jsonPath("$.data.apiUrl").value("****"))
                .andExpect(jsonPath("$.data.appSecret").value("****"));

        mockMvc.perform(get(BASE + "/api-config/" + id)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("****"));

        mockMvc.perform(get(BASE + "/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("wechatId", "test_wx_s08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].contactPhone").value("13800138008"));

        mockMvc.perform(put(BASE + "/update")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"id": %d, "accountName": "测试个微-更新"}
                                """, id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(delete(BASE + "/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("S-08: 企微 CRUD + secret 脱敏")
    void weworkCrudAndMask() throws Exception {
        mockMvc.perform(post(WEWORK + "/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountName": "测试企微",
                                  "corpId": "corp_s08",
                                  "agentId": "agent_s08",
                                  "secret": "my-secret-value",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get(WEWORK + "/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("corpId", "corp_s08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].secret").value("****"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("S-08b: 企微员工 CRUD")
    void weworkEmployeeCrud() throws Exception {
        String idStr = mockMvc.perform(post(WEWORK_EMP + "/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weworkAccountId": 9001,
                                  "nickname": "测试员工",
                                  "weworkUserId": "test_emp_s08b",
                                  "phone": "13900139088",
                                  "department": "测试部",
                                  "position": "测试岗",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        Number num = com.jayway.jsonpath.JsonPath.read(idStr, "$.data");
        Long id = num.longValue();

        mockMvc.perform(get(WEWORK_EMP + "/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("weworkAccountId", "9001")
                        .param("weworkUserId", "test_emp_s08b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].nickname").value("测试员工"));

        mockMvc.perform(put(WEWORK_EMP + "/update")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "id": %d,
                                  "nickname": "测试员工-更新",
                                  "weworkUserId": "test_emp_s08b",
                                  "department": "运营部",
                                  "position": "运营专员",
                                  "status": "ENABLED"
                                }
                                """, id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(delete(WEWORK_EMP + "/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
