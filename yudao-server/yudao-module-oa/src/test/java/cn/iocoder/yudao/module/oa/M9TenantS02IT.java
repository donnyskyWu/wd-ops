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
class M9TenantS02IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M9-S-02: 创建租户 + 列表")
    void createTenantAndList() throws Exception {
        createTenant("M9-测试租户A");

        mockMvc.perform(get("/admin-api/oa/system/tenant/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("tenantName", "M9-测试租户A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].status").value("TRIAL"))
                .andExpect(jsonPath("$.data.list[0].accountCount").value(0));
    }

    @Test
    @DisplayName("M9-S-02: 租户名称重复 (2006)")
    void duplicateTenantNameFails() throws Exception {
        createTenant("M9-重复租户");

        mockMvc.perform(post("/admin-api/oa/system/tenant/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "M9-重复租户",
                                  "contactName": "联系人",
                                  "contactPhone": "13800138888",
                                  "expireTime": "2027-12-31T23:59:59"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(2021));
    }

    @Test
    @DisplayName("M9-S-02: 非法租户状态 (1503)")
    void invalidTenantStatusFails() throws Exception {
        mockMvc.perform(post("/admin-api/oa/system/tenant/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "M9-非法状态",
                                  "contactName": "联系人",
                                  "contactPhone": "13800138777",
                                  "expireTime": "2027-12-31T23:59:59",
                                  "status": "INVALID"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1503));
    }

    @Test
    @DisplayName("M9-S-02: 更新租户状态 NORMAL")
    void updateTenantStatus() throws Exception {
        Long tenantId = createTenant("M9-更新租户");

        mockMvc.perform(put("/admin-api/oa/system/tenant/update")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"id": %d, "status": "NORMAL", "contactName": "新联系人"}
                                """, tenantId)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/system/tenant/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("tenantName", "M9-更新租户"))
                .andExpect(jsonPath("$.data.list[0].status").value("NORMAL"))
                .andExpect(jsonPath("$.data.list[0].contactName").value("新联系人"));
    }

    @Test
    @DisplayName("M9-S-02: 删除空租户 + 内置租户不可删")
    void deleteTenantRules() throws Exception {
        Long tenantId = createTenant("M9-待删租户");

        mockMvc.perform(delete("/admin-api/oa/system/tenant/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(tenantId)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(delete("/admin-api/oa/system/tenant/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", "1"))
                .andExpect(jsonPath("$.code").value(1502));
    }

    private Long createTenant(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/oa/system/tenant/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "tenantName": "%s",
                                  "contactName": "M9联系人",
                                  "contactPhone": "13800138666",
                                  "contactEmail": "m9@test.local",
                                  "expireTime": "2027-06-30T23:59:59",
                                  "maxAccounts": 20,
                                  "remark": "M9 IT"
                                }
                                """, name)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);
    }
}
