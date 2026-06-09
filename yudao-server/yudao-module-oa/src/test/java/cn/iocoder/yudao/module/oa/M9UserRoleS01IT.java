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
class M9UserRoleS01IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M9-S-01: 创建用户 + 列表 + 手机脱敏")
    void createUserAndList() throws Exception {
        Long roleId = createRole("M9_TEST_OPERATOR", "M9测试运营");

        createUser("m9_user_a", "M9测试用户A", roleId);

        mockMvc.perform(get("/admin-api/system/user/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("username", "m9_user_a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].phoneMasked").value("138****8001"))
                .andExpect(jsonPath("$.data.list[0].roleNames[0]").value("M9测试运营"));
    }

    @Test
    @DisplayName("M9-S-01: 用户名重复 (2006)")
    void duplicateUsernameFails() throws Exception {
        Long roleId = createRole("M9_TEST_DUP", "M9重复测试");
        createUser("m9_user_dup", "重复用户1", roleId);

        mockMvc.perform(post("/admin-api/system/user/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "username": "m9_user_dup",
                                  "nickname": "重复用户2",
                                  "phone": "13800138888",
                                  "status": "ENABLED",
                                  "roleIds": [%d]
                                }
                                """, roleId)))
                .andExpect(jsonPath("$.code").value(2006));
    }

    @Test
    @DisplayName("M9-S-01: 非法用户状态 (1503)")
    void invalidUserStatusFails() throws Exception {
        Long roleId = createRole("M9_TEST_STAT", "M9状态测试");

        mockMvc.perform(post("/admin-api/system/user/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "username": "m9_bad_status",
                                  "nickname": "状态非法",
                                  "status": "INVALID_STATUS",
                                  "roleIds": [%d]
                                }
                                """, roleId)))
                .andExpect(jsonPath("$.code").value(1503));
    }

    @Test
    @DisplayName("M9-S-01: 角色授权 + 权限点查询")
    void assignPermissionAndList() throws Exception {
        Long roleId = createRole("M9_TEST_PERM", "M9权限测试");

        mockMvc.perform(get("/admin-api/system/permission/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(21));

        mockMvc.perform(post("/admin-api/system/role/assign-permission")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"roleId": %d, "permissionIds": [1, 2, 3]}
                                """, roleId)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/system/role/permissions")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("roleId", String.valueOf(roleId)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @DisplayName("M9-S-01: 更新用户 + 删除用户")
    void updateAndDeleteUser() throws Exception {
        Long roleId = createRole("M9_TEST_UPD", "M9更新测试");
        Long userId = createUser("m9_user_upd", "待更新用户", roleId);

        mockMvc.perform(put("/admin-api/system/user/update")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"id": %d, "nickname": "已更新用户", "status": "DISABLED"}
                                """, userId)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(delete("/admin-api/system/user/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(userId)))
                .andExpect(jsonPath("$.code").value(0));
    }

    private Long createRole(String code, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/system/role/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"code": "%s", "name": "%s"}
                                """, code, name)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);
    }

    private Long createUser(String username, String nickname, Long roleId) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/system/user/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "username": "%s",
                                  "nickname": "%s",
                                  "phone": "13800138001",
                                  "position": "OPERATOR",
                                  "status": "ENABLED",
                                  "roleIds": [%d]
                                }
                                """, username, nickname, roleId)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);
    }
}
