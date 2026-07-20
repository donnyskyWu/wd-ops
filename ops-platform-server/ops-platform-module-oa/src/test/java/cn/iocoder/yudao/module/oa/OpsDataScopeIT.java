package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * OpsDataScopeSupport：账号类菜单 list/get fail-closed 校验。
 */
@AutoConfigureMockMvc
class OpsDataScopeIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String OPERATOR = "Bearer dev-token-oa-operator";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("管理员账号列表可见多 IP 组")
    void adminSeesAllIpGroupAccounts() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("platformType", "DOUYIN"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)));
    }

    @Test
    @DisplayName("运营专员仅见成员 IP 组 9001 账号")
    void operatorSeesMemberGroupAccountsOnly() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account/list")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .param("platformType", "DOUYIN"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.ipGroupId == 9001)]").exists())
                .andExpect(jsonPath("$.data.list[?(@.ipGroupId == 9002)]").doesNotExist());
    }

    @Test
    @DisplayName("运营专员不可读非权限组账号详情 → 403")
    void operatorCannotGetOtherGroupAccount() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account/get")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", "9006"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("运营专员可读本组账号详情")
    void operatorCanGetMemberGroupAccount() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account/get")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", "9001"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ipGroupId").value(9001));
    }
}
