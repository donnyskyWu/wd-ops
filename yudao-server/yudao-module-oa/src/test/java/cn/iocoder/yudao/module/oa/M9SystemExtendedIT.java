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
class M9SystemExtendedIT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M9: 系统参数 list + CRUD")
    void paramCrud() throws Exception {
        mockMvc.perform(get("/admin-api/oa/system/param/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("category", "BASIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(post("/admin-api/oa/system/param/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paramName": "测试参数",
                                  "paramKey": "test.m9.param",
                                  "paramValue": "123",
                                  "paramType": "NUMBER",
                                  "category": "BASIC"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M9: 字典 admin list + type-list")
    void dictAdminList() throws Exception {
        mockMvc.perform(get("/admin-api/oa/system/dict/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/admin-api/oa/system/dict/type-list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M9: 操作日志 + 登录日志查询")
    void logQueries() throws Exception {
        mockMvc.perform(get("/admin-api/oa/system/log/operation")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/admin-api/oa/system/log/login")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("M9: 数据写操作记录操作日志")
    void dataOperationAuditLog() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis() % 1_000_000);
        mockMvc.perform(post("/admin-api/oa/company/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "M9审计测试公司%s",
                                  "creditCode": "91330100M9%s",
                                  "industry": "测试行业",
                                  "status": "ENABLED",
                                  "mpCapacityStandard": 10
                                }
                                """.formatted(suffix, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/system/log/operation")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("module", "ACCOUNT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].module").value("ACCOUNT"))
                .andExpect(jsonPath("$.data.list[0].action").value("新增公司"))
                .andExpect(jsonPath("$.data.list[0].content").value("公司管理 / 新增公司"));
    }

    @Test
    @DisplayName("M9: 头部个人中心 profile")
    void userProfile() throws Exception {
        mockMvc.perform(get("/admin-api/oa/system/user/profile")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("oa-admin"))
                .andExpect(jsonPath("$.data.nickname").value("系统管理员"));
    }

    @Test
    @DisplayName("M9: 消息 list + send")
    void messageSend() throws Exception {
        mockMvc.perform(get("/admin-api/oa/system/message/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(post("/admin-api/oa/system/message/send")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "M9测试消息",
                                  "category": "SYSTEM",
                                  "channels": ["EMAIL"],
                                  "receiver": "admin@test.local",
                                  "content": "integration test"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M9: 头部消息未读列表 + 标记已读")
    void messageUnreadAndRead() throws Exception {
        var sendResult = mockMvc.perform(post("/admin-api/oa/system/message/send")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "头部未读消息测试",
                                  "category": "SYSTEM",
                                  "channels": ["STATION"],
                                  "receiver": "oa-admin",
                                  "content": "header unread integration test"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long messageId = extractDataId(sendResult.getResponse().getContentAsString());

        mockMvc.perform(get("/admin-api/oa/system/message/unread-count")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/admin-api/oa/system/message/unread")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].id").value(messageId))
                .andExpect(jsonPath("$.data.list[0].read").value(false));

        mockMvc.perform(put("/admin-api/oa/system/message/read")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(messageId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/system/message/get")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(messageId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.read").value(true));
    }

    @Test
    @DisplayName("M9: 字典停用项可删、启用项不可删")
    void dictDeleteRules() throws Exception {
        mockMvc.perform(post("/admin-api/oa/system/dict/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dictType": "dict_m9_test_del",
                                  "dictName": "M9删除测试",
                                  "items": [
                                    {"dictLabel": "启用项", "dictValue": "ENABLED_ITEM", "sort": 1, "status": "ENABLED"},
                                    {"dictLabel": "停用项", "dictValue": "DISABLED_ITEM", "sort": 2, "status": "DISABLED"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        var listResult = mockMvc.perform(get("/admin-api/oa/system/dict/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("dictType", "dict_m9_test_del"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String body = listResult.getResponse().getContentAsString();
        long enabledId = extractId(body, "ENABLED_ITEM");
        long disabledId = extractId(body, "DISABLED_ITEM");

        mockMvc.perform(delete("/admin-api/oa/system/dict/" + enabledId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1502));

        mockMvc.perform(delete("/admin-api/oa/system/dict/" + disabledId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private long extractId(String json, String dictValue) {
        int idx = json.indexOf("\"dictValue\":\"" + dictValue + "\"");
        if (idx < 0) {
            throw new IllegalStateException("dictValue not found: " + dictValue);
        }
        int idIdx = json.lastIndexOf("\"id\":", idx);
        int start = idIdx + 5;
        int end = json.indexOf(',', start);
        if (end < 0) {
            end = json.indexOf('}', start);
        }
        return Long.parseLong(json.substring(start, end).trim());
    }

    private long extractDataId(String json) {
        int idx = json.indexOf("\"data\":");
        if (idx < 0) {
            throw new IllegalStateException("data id not found");
        }
        int start = idx + 7;
        int end = json.indexOf(',', start);
        if (end < 0) {
            end = json.indexOf('}', start);
        }
        return Long.parseLong(json.substring(start, end).trim());
    }
}
