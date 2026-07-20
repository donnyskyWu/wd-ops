package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 平台账号创建：强制绑定 IP 组 + 数据范围校验。
 */
@AutoConfigureMockMvc
class M4AccountIpGroupScopeIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String OPERATOR = "Bearer dev-token-oa-operator";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("创建账号缺少 ipGroupId → 1400")
    void createWithoutIpGroupFails() throws Exception {
        mockMvc.perform(post("/admin-api/oa/account/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platformType": "DOUYIN",
                                  "accountName": "缺IP组",
                                  "externalAccountId": "dy_no_ip_%d",
                                  "companyId": 9001,
                                  "realnameId": 9001,
                                  "status": "NORMAL"
                                }
                                """.formatted(System.currentTimeMillis())))
                .andExpect(jsonPath("$.code").value(1400));
    }

    @Test
    @DisplayName("运营专员可绑定所属 IP 组 9001")
    void operatorCanBindMemberGroup() throws Exception {
        mockMvc.perform(post("/admin-api/oa/account/create")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platformType": "DOUYIN",
                                  "accountName": "专员绑定9001",
                                  "externalAccountId": "dy_op_9001_%d",
                                  "companyId": 9001,
                                  "realnameId": 9001,
                                  "ipGroupId": 9001,
                                  "status": "NORMAL"
                                }
                                """.formatted(System.currentTimeMillis())))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("运营专员不可绑定非权限 IP 组 9002 → 1504")
    void operatorCannotBindOtherGroup() throws Exception {
        mockMvc.perform(post("/admin-api/oa/account/create")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platformType": "DOUYIN",
                                  "accountName": "专员越权9002",
                                  "externalAccountId": "dy_op_9002_%d",
                                  "companyId": 9001,
                                  "realnameId": 9001,
                                  "ipGroupId": 9002,
                                  "status": "NORMAL"
                                }
                                """.formatted(System.currentTimeMillis())))
                .andExpect(jsonPath("$.code").value(1504));
    }

    @Test
    @DisplayName("accessible-tree：管理员全树 / 专员仅权限内组")
    void accessibleTreeScopedByRole() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/accessible-tree")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/admin-api/oa/ip-group/accessible-tree")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].children[?(@.id == 9001)]").exists())
                .andExpect(jsonPath("$.data[0].children[?(@.id == 9002)]").doesNotExist());
    }
}
