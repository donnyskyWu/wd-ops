package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M1AuthorS04IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M1-S-04: 新建作者")
    void createAuthor() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/oa/author/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorName": "IT-作者测试",
                                  "ipGroupId": 9001,
                                  "authorType": "SHORT_VIDEO",
                                  "status": 1
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        Long authorId = JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get("/admin-api/oa/author/" + authorId + "/dashboard")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.authorName").value("IT-作者测试"));
    }

    @Test
    @DisplayName("M1-S-04: 作者 IP 组必须小组 (1101)")
    void authorIpGroupMustSmall() throws Exception {
        mockMvc.perform(post("/admin-api/oa/author/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorName": "IT-非法大组作者",
                                  "ipGroupId": 9000,
                                  "authorType": "SHORT_VIDEO",
                                  "status": 1
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1101));
    }

    @Test
    @DisplayName("M1-S-04: 主推号类型校验 (1102)")
    void primaryAccountTypeInvalid() throws Exception {
        mockMvc.perform(post("/admin-api/oa/author/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorName": "IT-非法主推号",
                                  "ipGroupId": 9001,
                                  "authorType": "SHORT_VIDEO",
                                  "primaryAccountId": 9006,
                                  "status": 1
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1102));
    }
}
