package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M1OpsAnchorS05IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M1-S-05: 新建运营→主播关联")
    void createOpsAnchor() throws Exception {
        mockMvc.perform(post("/admin-api/oa/ops-anchor/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "opsUserId": 1002,
                                  "anchorUserId": 1003,
                                  "ipGroupId": 9001,
                                  "startDate": "2027-01-01",
                                  "endDate": "2027-06-30"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/ops/1002/anchor-stats")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.opsUserId").value(1002));
    }

    @Test
    @DisplayName("M1-S-05: 日期段重叠 (1201)")
    void overlapDates() throws Exception {
        mockMvc.perform(post("/admin-api/oa/ops-anchor/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "opsUserId": 1003,
                                  "anchorUserId": 1004,
                                  "ipGroupId": 9001,
                                  "startDate": "2026-06-01",
                                  "endDate": "2026-12-31"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1201));
    }
}
