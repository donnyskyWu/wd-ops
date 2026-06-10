package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S-R5：内部内容补录 - 完整流程 AC
 * 覆盖：
 *  - AC-M1-006-1 补录提交（待审核状态）
 *  - AC-M1-006-2 审核通过（trend 能查到该日数据）
 *  - AC-M1-006-3 审核驳回（状态变更）
 *  - AC-M1-006-4 90 天时间窗口（2025-01-01 应被拒）
 *  - importList 分页
 *  - 重复审核已通过 → 锁定
 */
@AutoConfigureMockMvc
class M1InternalContentImportIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S-R5 AC-M1-006-1: 补录提交成功，返回新 id")
    void submitImport() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/oa/internal-content/import")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contentId": 9301,
                                  "statDate": "2026-06-05",
                                  "importType": "API_EXCEPTION",
                                  "readCount": 8000,
                                  "likeCount": 200,
                                  "commentCount": 50,
                                  "forwardCount": 30,
                                  "remark": "IT-AC-006-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long id = JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);
        assertThat(id).isPositive();
    }

    @Test
    @DisplayName("S-R5 AC-M1-006-2: 审核通过 → trend 能查到该日 readCount")
    void reviewApprovedMergesToContentDaily() throws Exception {
        // 1) 提交
        MvcResult submit = mockMvc.perform(post("/admin-api/oa/internal-content/import")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contentId": 9301,
                                  "statDate": "2026-05-28",
                                  "importType": "API_EXCEPTION",
                                  "readCount": 12345,
                                  "likeCount": 100
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long id = JsonPath.parse(submit.getResponse().getContentAsString()).read("$.data", Long.class);

        // 2) 审核通过
        mockMvc.perform(put("/admin-api/oa/internal-content/import/" + id + "/review")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reviewStatus": 1, "remark": "IT approved"}
                                """))
                .andExpect(jsonPath("$.code").value(0));

        // 3) 验证 trend 包含 2026-05-28 且 readCount=12345
        MvcResult trend = mockMvc.perform(get("/admin-api/oa/internal-content/9301/trend")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String body = trend.getResponse().getContentAsString();
        Integer seriesLen = JsonPath.read(body, "$.data.series.length()");
        assertThat(seriesLen).isGreaterThanOrEqualTo(1);
        // 找到 2026-05-28 这一项
        for (int i = 0; i < seriesLen; i++) {
            String date = JsonPath.read(body, "$.data.series[" + i + "].date");
            if ("2026-05-28".equals(date)) {
                Number read = JsonPath.read(body, "$.data.series[" + i + "].readCount");
                assertThat(read.longValue()).isEqualTo(12345L);
                return;
            }
        }
        org.junit.jupiter.api.Assertions.fail("trend 不包含 2026-05-28 补录数据，series=" + seriesLen);
    }

    @Test
    @DisplayName("S-R5 AC-M1-006-3: 审核驳回 → status=2")
    void reviewRejected() throws Exception {
        MvcResult submit = mockMvc.perform(post("/admin-api/oa/internal-content/import")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contentId": 9301,
                                  "statDate": "2026-06-01",
                                  "importType": "OFFLINE",
                                  "readCount": 500
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long id = JsonPath.parse(submit.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(put("/admin-api/oa/internal-content/import/" + id + "/review")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reviewStatus": 2, "remark": "数据与 API 不符"}
                                """))
                .andExpect(jsonPath("$.code").value(0));

        // 验证 importList 里 status=2
        MvcResult list = mockMvc.perform(get("/admin-api/oa/internal-content/import/list?reviewStatus=2")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Integer total = JsonPath.read(list.getResponse().getContentAsString(), "$.data.total");
        assertThat(total).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("S-R5 AC-M1-006-4: 补录日期 > 90 天前 → 1301 拒绝")
    void importDateTooOld() throws Exception {
        mockMvc.perform(post("/admin-api/oa/internal-content/import")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contentId": 9301,
                                  "statDate": "2025-01-01",
                                  "importType": "API_EXCEPTION",
                                  "readCount": 100
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1301));
    }

    @Test
    @DisplayName("S-R5: 已通过补录锁定不可再审")
    void approvedImportIsLocked() throws Exception {
        // 提交 + 通过
        MvcResult submit = mockMvc.perform(post("/admin-api/oa/internal-content/import")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contentId": 9301,
                                  "statDate": "2026-06-02",
                                  "importType": "API_EXCEPTION",
                                  "readCount": 100
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long id = JsonPath.parse(submit.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(put("/admin-api/oa/internal-content/import/" + id + "/review")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reviewStatus": 1, "remark": "first approved"}
                                """))
                .andExpect(jsonPath("$.code").value(0));

        // 再次审核应被拒
        mockMvc.perform(put("/admin-api/oa/internal-content/import/" + id + "/review")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reviewStatus": 2, "remark": "second review"}
                                """))
                .andExpect(jsonPath("$.code").value(1304));
    }
}
