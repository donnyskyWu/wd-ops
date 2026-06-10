package cn.iocoder.yudao.module.oa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P-GATE-UNMOCK S-E: IP 组编辑时修改 parentId 的支持。
 * 之前 spec 缺字段，前端改了不生效。
 */
@AutoConfigureMockMvc
class M1IpGroupUpdateParentIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    /** 创建一个新大组（用于测试切换父组） */
    private Long createBigGroup(String name) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("groupName", name);
        body.put("groupType", 1);
        body.put("status", 1);
        body.put("sortOrder", 0);
        body.put("remark", "test");
        MvcResult r = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin-api/oa/ip-group/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("data").asLong();
    }

    /** 创建一个新小组（parent 默认为已有大组 9000） */
    private Long createSmallGroup(String name, Long parentId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("groupName", name);
        body.put("groupType", 2);
        body.put("parentId", parentId);
        body.put("status", 1);
        body.put("sortOrder", 0);
        body.put("remark", "test");
        MvcResult r = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin-api/oa/ip-group/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("data").asLong();
    }

    @Test
    @DisplayName("S-E-1: 小组改 parentId 到另一个大组 → 200, DB 持久化")
    void updateParentIdToAnotherBigGroup() throws Exception {
        // 准备：新建大组 B + 小组 child（原 parent=9000）
        Long bigB = createBigGroup("SEED-TEST-S-E-B");
        Long child = createSmallGroup("SEED-TEST-S-E-child", 9000L);

        Map<String, Object> body = new HashMap<>();
        body.put("id", child);
        body.put("parentId", bigB);

        mockMvc.perform(put("/admin-api/oa/ip-group/update")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        // 验证 getDetail 返回新 parentId
        mockMvc.perform(get("/admin-api/oa/ip-group/" + child)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.parentId").value(bigB));
    }

    @Test
    @DisplayName("S-E-2: 改 parentId 为自己 → 1003 IP_GROUP_PARENT_INVALID")
    void updateParentIdToSelfRejected() throws Exception {
        Long bigB = createBigGroup("SEED-TEST-S-E-selfB");
        Long child = createSmallGroup("SEED-TEST-S-E-self", 9000L);

        Map<String, Object> body = new HashMap<>();
        body.put("id", child);
        body.put("parentId", child); // 自引用

        mockMvc.perform(put("/admin-api/oa/ip-group/update")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    @DisplayName("S-E-3: parentId 指向不存在/跨租户的大组 → 1003")
    void updateParentIdToInvalidBigGroup() throws Exception {
        Long child = createSmallGroup("SEED-TEST-S-E-invalid", 9000L);

        Map<String, Object> body = new HashMap<>();
        body.put("id", child);
        body.put("parentId", 999999L); // 不存在

        mockMvc.perform(put("/admin-api/oa/ip-group/update")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    @DisplayName("S-E-4: 大组改 parentId（必须 null）→ 1003")
    void updateBigGroupParentIdRejected() throws Exception {
        Long big = createBigGroup("SEED-TEST-S-E-bigonly");

        Map<String, Object> body = new HashMap<>();
        body.put("id", big);
        body.put("parentId", 9000L); // 大组不能有 parent

        mockMvc.perform(put("/admin-api/oa/ip-group/update")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    @DisplayName("S-E-5: parentId=null（不动）保持原样")
    void updateWithoutParentIdKeepsOriginal() throws Exception {
        Long child = createSmallGroup("SEED-TEST-S-E-keep", 9000L);

        // 不传 parentId → 应不修改
        Map<String, Object> body = new HashMap<>();
        body.put("id", child);
        body.put("groupName", "SEED-TEST-S-E-keep-renamed");

        mockMvc.perform(put("/admin-api/oa/ip-group/update")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/ip-group/" + child)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.groupName").value("SEED-TEST-S-E-keep-renamed"))
                .andExpect(jsonPath("$.data.parentId").value(9000));
    }
}
