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
class M9DeptS01IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M9-Dept: 部门 CRUD + 树 + 用户部门关联")
    void deptCrudAndUserDept() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/system/dept/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "M9测试部门", "sort": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long deptId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get("/admin-api/oa/system/dept/tree")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.name=='M9测试部门')]").exists());

        Long roleId = createRole("M9_DEPT_ROLE", "M9部门测试角色");
        Long userId = createUser("m9_dept_user", "部门测试用户", roleId, deptId);

        mockMvc.perform(get("/admin-api/oa/system/user/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("deptId", String.valueOf(deptId)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].deptName").value("M9测试部门"));

        mockMvc.perform(put("/admin-api/oa/system/dept/update")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"id": %d, "name": "M9测试部门-改"}
                                """, deptId)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(delete("/admin-api/oa/system/dept/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(deptId)))
                .andExpect(jsonPath("$.code").value(1502));

        mockMvc.perform(delete("/admin-api/oa/system/user/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(userId)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(delete("/admin-api/oa/system/dept/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(deptId)))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M9-Dept: 钉钉未配置时同步失败")
    void syncWithoutConfigFails() throws Exception {
        mockMvc.perform(post("/admin-api/oa/system/dept/sync-dingtalk")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(1400));
    }

    private Long createRole(String code, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/oa/system/role/create")
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

    private Long createUser(String username, String nickname, Long roleId, Long deptId) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/oa/system/user/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "username": "%s",
                                  "nickname": "%s",
                                  "phone": "13800138002",
                                  "position": "OPERATOR",
                                  "status": "ENABLED",
                                  "deptId": %d,
                                  "roleIds": [%d]
                                }
                                """, username, nickname, deptId, roleId)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);
    }
}
