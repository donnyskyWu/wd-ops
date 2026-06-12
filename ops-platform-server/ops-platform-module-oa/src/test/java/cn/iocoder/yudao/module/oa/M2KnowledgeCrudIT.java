package cn.iocoder.yudao.module.oa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P-GATE-UNMOCK-R S-R1 P0-1：补齐 M2 知识库 4 个 Controller 方法 + 前端 URL 修复 + ADR-007 妥协。
 * 覆盖：detail / update / delete / like
 */
@AutoConfigureMockMvc
class M2KnowledgeCrudIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper om;

    private Long createOne() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "IT-S-R1-Knowledge");
        body.put("category", "TEMPLATE_LIB"); // 后端 dict_knowledge_category 合法值
        body.put("content", "<p>富文本</p>");
        body.put("tags", "tagA,tagB");
        body.put("isPublic", 1);
        MvcResult r = mockMvc.perform(post("/admin-api/oa/knowledge/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(r.getResponse().getContentAsString()).read("$.data", Long.class);
    }

    @Test
    @DisplayName("P0-1: GET /oa/knowledge/{id} 200")
    void getById() throws Exception {
        Long id = createOne();
        mockMvc.perform(get("/admin-api/oa/knowledge/" + id)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.title").value("IT-S-R1-Knowledge"))
                .andExpect(jsonPath("$.data.category").value("TEMPLATE_LIB"))
                .andExpect(jsonPath("$.data.creatorName").exists());
    }

    @Test
    @DisplayName("P0-1: GET /oa/knowledge/999999 1500 实体不存在")
    void getByIdNotFound() throws Exception {
        mockMvc.perform(get("/admin-api/oa/knowledge/999999")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1500));
    }

    @Test
    @DisplayName("P0-1: PUT /oa/knowledge/update 200 + 持久化")
    void update() throws Exception {
        Long id = createOne();
        Map<String, Object> body = new HashMap<>();
        body.put("id", id);
        body.put("title", "IT-S-R1-Knowledge-RENAMED");
        body.put("category", "OPS_TIPS");
        body.put("content", "<p>改后内容</p>");
        body.put("tags", "tagC");
        body.put("isPublic", 0);

        mockMvc.perform(put("/admin-api/oa/knowledge/update")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        // 验证
        mockMvc.perform(get("/admin-api/oa/knowledge/" + id)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("IT-S-R1-Knowledge-RENAMED"))
                .andExpect(jsonPath("$.data.category").value("OPS_TIPS"))
                .andExpect(jsonPath("$.data.isPublic").value(0));
    }

    @Test
    @DisplayName("P0-1: DELETE /oa/knowledge/delete?id=X 200 + 真删")
    void deleteKnowledge() throws Exception {
        Long id = createOne();
        mockMvc.perform(delete("/admin-api/oa/knowledge/delete")
                        .param("id", String.valueOf(id))
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        // 验证删除
        mockMvc.perform(get("/admin-api/oa/knowledge/" + id)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(1500));
    }

    @Test
    @DisplayName("P0-1: POST /oa/knowledge/like like / unlike")
    void like() throws Exception {
        Long id = createOne();

        Map<String, Object> like = new HashMap<>();
        like.put("id", id);
        like.put("action", "like");
        mockMvc.perform(post("/admin-api/oa/knowledge/like")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(like)))
                .andExpect(jsonPath("$.code").value(0));

        Map<String, Object> unlike = new HashMap<>();
        unlike.put("id", id);
        unlike.put("action", "unlike");
        mockMvc.perform(post("/admin-api/oa/knowledge/like")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(unlike)))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("P0-1: POST /oa/knowledge/like action 非法 → 1400")
    void likeInvalidAction() throws Exception {
        Long id = createOne();
        Map<String, Object> body = new HashMap<>();
        body.put("id", id);
        body.put("action", "ping");
        mockMvc.perform(post("/admin-api/oa/knowledge/like")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(jsonPath("$.code").value(1400));
    }
}
