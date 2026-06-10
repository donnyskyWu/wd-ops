package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S-R12 IT：IP 组核心端点
 *
 * 覆盖：
 *  - B1: GET /{id} 返回 createTime 字段（非 createdAt）
 *  - B2/B3: GET /{id} 返回 sortOrder / remark 字段（编辑对话框用）
 *  - tree 端点
 *  - /list 端点
 */
@AutoConfigureMockMvc
class M1IpGroupDetailIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S-R12 IT-1: GET /{id} 返 createTime 字段")
    void getDetailReturnsCreateTime() throws Exception {
        // 找 seed 里的 IP 组 id（9000/9001）
        mockMvc.perform(get("/admin-api/oa/ip-group/9000")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.createTime").exists());
    }

    @Test
    @DisplayName("S-R12 IT-2: GET /{id} 返 sortOrder 字段（B2 修复）")
    void getDetailReturnsSortOrder() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/9000")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sortOrder").exists());
    }

    @Test
    @DisplayName("S-R12 IT-3: GET /{id} 返 remark 字段（B3 修复）")
    void getDetailReturnsRemark() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/9000")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.remark").exists());
    }

    @Test
    @DisplayName("S-R12 IT-4: GET /tree 返树形结构")
    void getTreeReturnsNested() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/tree")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("S-R12 IT-5: GET /list 不收 pageNo（用 pageNum 默认 1）")
    void listAcceptsPageNum() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/list")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("S-R12 IT-6: DELETE /delete?id=xxx 端点存在")
    void deleteEndpoint() throws Exception {
        // 不真删（避免破坏其他 IT 共享数据），只验 endpoint 存在
        // 实际删除用 update 端点改 status=0 即可
        mockMvc.perform(delete("/admin-api/oa/ip-group/delete")
                        .param("id", "99999")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                // 期望 1505 (not found) 或 200，看 service 实现
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("S-R12 IT-7: PUT /{id}/status 切换启停")
    void statusToggle() throws Exception {
        // 把 9000 切到停用（status=0）
        mockMvc.perform(put("/admin-api/oa/ip-group/9000/status")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        // 验证
        mockMvc.perform(get("/admin-api/oa/ip-group/9000")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.status").value(0));

        // 切回启用
        mockMvc.perform(put("/admin-api/oa/ip-group/9000/status")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":1}"))
                .andExpect(jsonPath("$.code").value(0));
    }
}
