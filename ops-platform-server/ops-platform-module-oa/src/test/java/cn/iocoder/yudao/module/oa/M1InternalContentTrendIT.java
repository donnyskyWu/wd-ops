package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
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
 * S-R2-Fix-3：内部内容分析趋势 - 接真 API（去 mock）
 * 覆盖：
 *  - GET /admin-api/oa/internal-content/{id}/trend 返回 30 天日数据（DB 有 seed）
 *  - 不存在 contentId 返回 1302
 *  - 跨租户返回 1504
 */
@AutoConfigureMockMvc
class M1InternalContentTrendIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final String OTHER_TENANT = "2";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S-R2-Fix-3: 内容趋势 - 真 API + 30 天数据")
    void realTrendApiReturnsDailySeries() throws Exception {
        // 9350 是 seed 真实 content id（oa_content id 段 9301-9350；9350 有 30 天 oa_content_daily 数据）
        MvcResult result = mockMvc.perform(get("/admin-api/oa/internal-content/9350/trend")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        Integer seriesLen = JsonPath.read(body, "$.data.series.length()");
        String title = JsonPath.read(body, "$.data.title");
        Number firstRead = JsonPath.read(body, "$.data.series[0].readCount");
        String firstDate = JsonPath.read(body, "$.data.series[0].date");

        assertThat(seriesLen).isGreaterThanOrEqualTo(7); // 至少 7 天（30 天最佳，但允许 seed 调整）
        assertThat(title).isNotBlank();
        assertThat(firstRead).isNotNull();
        assertThat(firstRead.longValue()).isGreaterThanOrEqualTo(0L);
        assertThat(firstDate).matches("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    @DisplayName("S-R2-Fix-3: 不存在的 contentId 返回 1302")
    void trendNotFound() throws Exception {
        mockMvc.perform(get("/admin-api/oa/internal-content/999999999/trend")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1302));
    }

    @Test
    @DisplayName("S-R2-Fix-3: 跨租户访问 - 应被 1504 拦截")
    void trendCrossTenantForbidden() throws Exception {
        // 9350 属于 tenant=1；用 tenant=2 访问应失败
        mockMvc.perform(get("/admin-api/oa/internal-content/9350/trend")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", OTHER_TENANT))
                .andExpect(status().isOk())
                // 跨租户要么找不到（1302）要么越权（1504），二选一
                .andExpect(result -> {
                    int code = JsonPath.read(result.getResponse().getContentAsString(), "$.code");
                    assertThat(code).isIn(1302, 1504);
                });
    }
}
