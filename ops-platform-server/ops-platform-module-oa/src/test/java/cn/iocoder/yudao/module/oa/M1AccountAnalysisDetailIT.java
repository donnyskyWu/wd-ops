package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S-R13 IT：账号分析详情端点
 *
 * 覆盖：
 *  - 详情 /{id}/followers 端点（含 startDate/endDate 可选参数）
 *  - 详情 /{id}/contents 端点（page/size 风格，非 pageNum/pageSize）
 */
@AutoConfigureMockMvc
class M1AccountAnalysisDetailIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S-R13 IT-1: GET /{id}/followers 返数组（用 seed 账号 9001）")
    void followers() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account-analysis/9001/followers")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("S-R13 IT-2: followers 端点支持 startDate/endDate 过滤")
    void followersWithDateFilter() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account-analysis/9001/followers")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-06-30")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("S-R13 IT-3: GET /{id}/contents 返分页结构")
    void contents() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account-analysis/9001/contents")
                        .param("page", "1")
                        .param("size", "20")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").exists());
    }

    @Test
    @DisplayName("S-R13 IT-4: contents 端点不收 pageNum（确认是 page/size 风格）")
    void contentsIgnoresPageNum() throws Exception {
        // pageNum 是其他模块的命名，此端点用 page/size
        // 不应 500，Spring 严格按名匹配，pageNum=99 不会影响
        mockMvc.perform(get("/admin-api/oa/account-analysis/9001/contents")
                        .param("pageNum", "99")
                        .param("size", "10")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
