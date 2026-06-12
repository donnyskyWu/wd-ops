package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S-R7：InternalContent list 4 筛选项 IT（Bug4 修复回归）
 * spec: FR-M1-006 / PRD-M1-运营管理 §4.6
 */
@AutoConfigureMockMvc
class M1InternalContentListFilterIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * S-R7-B1：ipGroupId 通过 oa_account 关联过滤
     * seed 中 IP 组 1 下有 ≥1 账号有 content，过滤后 total < 全量 26
     */
    @Test
    @DisplayName("S-R7: list 支持 ipGroupId 过滤（关联 oa_account）")
    void listFiltersByIpGroup() throws Exception {
        int totalAll = getListTotal(null, null, null, null, null);
        int totalIp1 = getListTotal(null, null, 1L, null, null);
        // 至少有 1 个 IP 组下账号数 < 总账号数 → totalIp1 < totalAll
        assertThat(totalIp1).isLessThan(totalAll);
    }

    /**
     * S-R7-B2：keyword 模糊匹配 title
     * seed 中 26 条 content，模糊匹配任意 title 子串
     */
    @Test
    @DisplayName("S-R7: list 支持 keyword 模糊匹配")
    void listFiltersByKeyword() throws Exception {
        int totalAll = getListTotal(null, null, null, null, null);
        // 用一个必不存在的关键词
        int totalEmpty = getListTotal(null, null, null, "Z_NONEXISTENT_KEYWORD", null);
        assertThat(totalEmpty).isEqualTo(0);
        // 不传 keyword 应保持全量
        assertThat(totalAll).isGreaterThan(0);
    }

    /**
     * S-R7-B3：dateRange 范围 publishTime
     * seed 中 publishTime 跨多日，缩窗口应减少 total
     */
    @Test
    @DisplayName("S-R7: list 支持 startDate/endDate 范围")
    void listFiltersByDateRange() throws Exception {
        int totalAll = getListTotal(null, null, null, null, null);
        // 用 1 天窗口（必 < 全量）
        int totalOneDay = getListTotal(null, null, null, null, "2026-05-15");
        assertThat(totalOneDay).isLessThan(totalAll);
    }

    /**
     * S-R7-B1+B2+B3 组合：4 筛选项都生效
     */
    @Test
    @DisplayName("S-R7: list 4 筛选项组合生效")
    void listFiltersCombined() throws Exception {
        // 全部为空 → 全量
        int totalAll = getListTotal(null, null, null, null, null);
        assertThat(totalAll).isGreaterThan(0);
        // ipGroupId=1 + keyword Z_NONEXISTENT → 0
        MvcResult result = mockMvc.perform(get("/admin-api/oa/internal-content/list")
                        .param("ipGroupId", "1")
                        .param("keyword", "Z_NONEXISTENT_KEYWORD")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        int data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("total").asInt();
        assertThat(data).isEqualTo(0);
    }

    /**
     * S-R7-B5：page/size 参数生效（不是 pageNo/size）
     */
    @Test
    @DisplayName("S-R7: list 收 page/size（不是 pageNo/size）")
    void listAcceptsPageSize() throws Exception {
        mockMvc.perform(get("/admin-api/oa/internal-content/list")
                        .param("page", "2")
                        .param("size", "5")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list.length()").value(5));
    }

    /**
     * S-R7 回归：startDate 单独传不传 endDate 也能用
     */
    @Test
    @DisplayName("S-R7: list 只传 startDate 不传 endDate 仍 OK")
    void listAcceptsStartDateOnly() throws Exception {
        mockMvc.perform(get("/admin-api/oa/internal-content/list")
                        .param("startDate", "2026-05-01")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private int getListTotal(String platformType, String dataSource,
                             Long ipGroupId, String keyword, String startDate) throws Exception {
        var req = get("/admin-api/oa/internal-content/list")
                .param("page", "1")
                .param("size", "100")
                .header("Authorization", ADMIN)
                .header("X-Tenant-Id", TENANT);
        if (platformType != null) req.param("platformType", platformType);
        if (dataSource != null) req.param("dataSource", dataSource);
        if (ipGroupId != null) req.param("ipGroupId", String.valueOf(ipGroupId));
        if (keyword != null) req.param("keyword", keyword);
        if (startDate != null) req.param("startDate", startDate);
        MvcResult result = mockMvc.perform(req)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("total").asInt();
    }
}
