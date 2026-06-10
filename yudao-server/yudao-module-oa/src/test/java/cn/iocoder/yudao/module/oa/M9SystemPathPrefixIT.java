package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * S-R23-Mike：M9 系统 API 路径前缀 — 规范路径 + 兼容别名
 */
@AutoConfigureMockMvc
class M9SystemPathPrefixIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S-R23: 规范路径 /oa/system/user/list 可用")
    void canonicalUserList() throws Exception {
        mockMvc.perform(get("/admin-api/oa/system/user/list")
                        .param("pageNo", "1")
                        .param("pageSize", "5")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("S-R23: 兼容别名 /system/user/list 仍可用")
    void legacyUserListAlias() throws Exception {
        mockMvc.perform(get("/admin-api/system/user/list")
                        .param("pageNo", "1")
                        .param("pageSize", "5")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("S-R23: 规范路径 /oa/system/permission/list 可用")
    void canonicalPermissionList() throws Exception {
        mockMvc.perform(get("/admin-api/oa/system/permission/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }
}
