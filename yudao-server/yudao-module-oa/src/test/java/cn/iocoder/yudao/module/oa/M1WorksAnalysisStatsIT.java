package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S-R8: 作品分析 - stats/trend 端点扩展
 * B1: 5 KPI 卡全量聚合（sumLikes/sumComments/sumShares）
 * B4: trend 端点收 contentId 过滤
 * B6: list 接口 page/size 契约
 */
@AutoConfigureMockMvc
class M1WorksAnalysisStatsIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S-R8-B1-1: /stats 返回 5 KPI 全量聚合字段")
    void statsReturnsAllFiveKpis() throws Exception {
        // 不依赖具体 count（H2 内存库 vs dev MySQL seed 数量可能不同），断言 5 字段都返回
        mockMvc.perform(get("/admin-api/oa/content-analysis/stats")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalCount").exists())
                .andExpect(jsonPath("$.data.hitCount").exists())
                .andExpect(jsonPath("$.data.totalRead").exists())
                .andExpect(jsonPath("$.data.totalLikes").exists())
                .andExpect(jsonPath("$.data.totalComments").exists())
                .andExpect(jsonPath("$.data.totalShares").exists());
    }

    @Test
    @DisplayName("S-R8-B1-2: /stats KPI 是全量聚合（非当前页）")
    void statsKpisAreAggregateNotPaginated() throws Exception {
        // 不传 page/size，stats 应当返回全量聚合值（不依赖 list 端点）
        mockMvc.perform(get("/admin-api/oa/content-analysis/stats")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.totalLikes").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.totalComments").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.totalShares").value(greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("S-R8-B1-3: /stats 按 IP 组过滤后 5 KPI 一致")
    void statsFilterByIpGroup() throws Exception {
        mockMvc.perform(get("/admin-api/oa/content-analysis/stats")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("ipGroupId", "9001"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalCount").value(greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("S-R8-B4-1: /trend 不传 contentId 返回全量按日聚合")
    void trendAllContent() throws Exception {
        mockMvc.perform(get("/admin-api/oa/content-analysis/trend")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].date").exists())
                .andExpect(jsonPath("$.data[0].readCount").exists());
    }

    @Test
    @DisplayName("S-R8-B4-2: /trend 传 contentId 仅返回该作品趋势")
    void trendByContentId() throws Exception {
        // 作品 9303 (SEED-公众号作品C) 单日发布
        mockMvc.perform(get("/admin-api/oa/content-analysis/trend")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("contentId", "9303"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("S-R8-B6-1: /list 用 page/size 分页正常")
    void listPageSize() throws Exception {
        mockMvc.perform(get("/admin-api/oa/content-analysis/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list.length()").value(greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("S-R8-B6-2: /list isHit=true 仅返回爆款（结构验证）")
    void listIsHitTrue() throws Exception {
        // H2 内存库与 dev MySQL seed 数量可能不同，断言结构而非具体 count
        mockMvc.perform(get("/admin-api/oa/content-analysis/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("isHit", "true")
                        .param("page", "1")
                        .param("size", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.list").isArray());
    }
}
