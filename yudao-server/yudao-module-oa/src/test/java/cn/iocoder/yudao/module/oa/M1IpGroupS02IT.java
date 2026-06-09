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
class M1IpGroupS02IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M1-S-02: 新建大组 + 小组")
    void createBigAndSmallGroup() throws Exception {
        Long bigId = createGroup("""
                {
                  "groupName": "IT-娱乐八卦大组",
                  "groupType": 1,
                  "parentId": null,
                  "leaderId": 1002,
                  "status": 1
                }
                """);

        Long smallId = createGroup(String.format("""
                {
                  "groupName": "IT-八卦一组",
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
                .andExpect(jsonPath("$.data.groupName").value("IT-八卦一组"))
                .andExpect(jsonPath("$.data.parentName").value("IT-娱乐八卦大组"));
    }

    @Test
    @DisplayName("M1-S-02: 同父级名称重复 (1002)")
    void duplicateName() throws Exception {
        Long bigId = createGroup("""
                {
                  "groupName": "IT-重复测试大组",
                  "groupType": 1,
                  "status": 1
                }
                """);

        mockMvc.perform(post("/admin-api/oa/ip-group/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupName": "IT-重复测试大组",
                                  "groupType": 1,
                                  "status": 1
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1002));

        mockMvc.perform(post("/admin-api/oa/ip-group/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "groupName": "IT-重复小组",
                                  "groupType": 2,
                                  "parentId": %d,
                                  "status": 1
                                }
                                """, bigId)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/admin-api/oa/ip-group/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "groupName": "IT-重复小组",
                                  "groupType": 2,
                                  "parentId": %d,
                                  "status": 1
                                }
                                """, bigId)))
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    @DisplayName("M1-S-02: 删除有账号的 IP 组 (1005)")
    void deleteProtectedGroup() throws Exception {
        mockMvc.perform(delete("/admin-api/oa/ip-group/delete")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", "9001"))
                .andExpect(jsonPath("$.code").value(1005));
    }

    @Test
    @DisplayName("M1-S-02: 删除空 IP 组 + 更新状态")
    void deleteEmptyGroupAndUpdateStatus() throws Exception {
        Long groupId = createGroup("""
                {
                  "groupName": "IT-待删除空组",
                  "groupType": 1,
                  "status": 1
                }
                """);

        mockMvc.perform(put("/admin-api/oa/ip-group/" + groupId + "/status")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": 0}"))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(delete("/admin-api/oa/ip-group/delete")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(groupId)))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M1-S-02: 成员增删改")
    void memberCrud() throws Exception {
        Long bigId = createGroup("""
                {
                  "groupName": "IT-成员测试大组",
                  "groupType": 1,
                  "status": 1
                }
                """);
        Long smallId = createGroup(String.format("""
                {
                  "groupName": "IT-成员测试小组",
                  "groupType": 2,
                  "parentId": %d,
                  "status": 1
                }
                """, bigId));

        mockMvc.perform(post("/admin-api/oa/ip-group/" + smallId + "/members")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1003,
                                  "position": "OPERATOR",
                                  "isLeader": false
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        MvcResult listResult = mockMvc.perform(get("/admin-api/oa/ip-group/" + smallId + "/members")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].userName").value("运营专员"))
                .andReturn();

        Long memberId = JsonPath.parse(listResult.getResponse().getContentAsString())
                .read("$.data[0].memberId", Long.class);

        mockMvc.perform(put("/admin-api/oa/ip-group/" + smallId + "/members/" + memberId)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\": \"OPS_LEADER\", \"isLeader\": true}"))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(delete("/admin-api/oa/ip-group/" + smallId + "/members/" + memberId)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/ip-group/" + smallId + "/members")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("M1-S-02: 小组上级必须是大组 (1003)")
    void invalidParentType() throws Exception {
        mockMvc.perform(post("/admin-api/oa/ip-group/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupName": "IT-非法子组",
                                  "groupType": 2,
                                  "parentId": 9001,
                                  "status": 1
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1003));
    }

    private Long createGroup(String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/oa/ip-group/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);
    }
}
