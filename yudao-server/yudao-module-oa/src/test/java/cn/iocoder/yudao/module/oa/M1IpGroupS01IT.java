package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class M1IpGroupS01IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String OPERATOR = "Bearer dev-token-oa-operator";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M1-S-01: IP 组树返回 SEED 大组 + 2 小组")
    void ipGroupTree() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/tree")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].groupName").value("SEED-运营大组"))
                .andExpect(jsonPath("$.data[0].groupType").value(1))
                .andExpect(jsonPath("$.data[0].children.length()").value(2))
                .andExpect(jsonPath("$.data[0].accountCount").value(10));
    }

    @Test
    @DisplayName("M1-S-01: IP 组详情 9001 含账号统计")
    void ipGroupDetail() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/9001")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.groupName").value("SEED-八卦一组"))
                .andExpect(jsonPath("$.data.groupType").value(2))
                .andExpect(jsonPath("$.data.parentName").value("SEED-运营大组"))
                .andExpect(jsonPath("$.data.accountCount").value(5));
    }

    @Test
    @DisplayName("M1-S-01: IP 组统计 Tab 骨架")
    void ipGroupStats() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/9001/stats")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accountCount").value(5))
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andExpect(jsonPath("$.data.totalFollowers").value(0));
    }

    @Test
    @DisplayName("M1-S-01: 关联账号列表来自 oa_account.ip_group_id")
    void ipGroupAccounts() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/9001/accounts")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(5));
    }

    @Test
    @DisplayName("M1-S-01: 成员/主播 Tab 返回 seed 数据")
    void memberAndAnchorTabs() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/9001/members")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/admin-api/oa/ip-group/9001/anchors")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("M1-S-01: operator 数据范围仍仅见 IP 组 9001 账号")
    void operatorDataScopeWithIpGroupTree() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/9001/accounts")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(5));

        mockMvc.perform(get("/admin-api/oa/ip-group/9002/accounts")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
