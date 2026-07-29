package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * IP 组管理：系统管理员全量访问 + 组长候选人/校验（V160 数据范围回归）。
 */
@AutoConfigureMockMvc
class M1IpGroupAdminScopeIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("6159: 系统管理员可访问树/详情/成员/统计")
    void adminCanManageIpGroupEndpoints() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/tree")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/admin-api/oa/ip-group/9001")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(9001));

        mockMvc.perform(get("/admin-api/oa/ip-group/9001/members")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/ip-group/9001/stats")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("6159: IP组长可读管辖小组及树祖先大组；祖先不可写")
    void leaderCanReadLedGroupAndAncestorButNotWriteAncestor() throws Exception {
        String leader = "Bearer dev-token-oa-leader";

        // 管辖小组 9001
        mockMvc.perform(get("/admin-api/oa/ip-group/9001/members")
                        .header("Authorization", leader)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/admin-api/oa/ip-group/9001/stats")
                        .header("Authorization", leader)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        // 树祖先大组 9000（getTree 可见，点击不应 403）
        mockMvc.perform(get("/admin-api/oa/ip-group/9000")
                        .header("Authorization", leader)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/admin-api/oa/ip-group/9000/members")
                        .header("Authorization", leader)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/admin-api/oa/ip-group/9000/stats")
                        .header("Authorization", leader)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        // 祖先只读：禁用状态变更应 403
        mockMvc.perform(put("/admin-api/oa/ip-group/9000/status")
                        .header("Authorization", leader)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("6159: 组长候选人含 ip_group_leader 用户")
    void leaderCandidateIdsIncludeIpGroupLeaderRole() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/leader-candidate-ids")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[?(@ == '1002' || @ == '1003')]").exists());
    }

    @Test
    @DisplayName("6159: 添加成员候选人返回租户内多用户（非数据权限裁剪）")
    void memberCandidatesReturnMultipleTenantUsers() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/member-candidates")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].nickname").exists());
    }

    @Test
    @DisplayName("6159: 系统管理员创建小组并指定 IP组长 用户为组长")
    void adminCreateGroupWithIpGroupLeader() throws Exception {
        Long bigId = createGroup("""
                {
                  "groupName": "IT-AdminScope-大组",
                  "groupType": 1,
                  "status": 1
                }
                """);

        Long smallId = createGroup(String.format("""
                {
                  "groupName": "IT-AdminScope-小组",
                  "groupType": 2,
                  "parentId": %d,
                  "leaderId": 1003,
                  "status": 1
                }
                """, bigId));

        mockMvc.perform(get("/admin-api/oa/ip-group/" + smallId)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.leaderId").value(1003));
    }

    private Long createGroup(String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/oa/ip-group/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);
    }
}
