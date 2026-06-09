package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M3OrderAttributionS06IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M3-S-06: 订单归因列表")
    void listAttribution() throws Exception {
        mockMvc.perform(get("/admin-api/oa/order-attribution/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-08"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(10)))
                .andExpect(jsonPath("$.data.list[0].authorId").exists());
    }

    @Test
    @DisplayName("M3-S-06: ROI 聚合")
    void roiAggregate() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/oa/order-attribution/roi")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-08"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.byIpGroup.length()").value(greaterThanOrEqualTo(1)))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        Number payAmount = JsonPath.read(json, "$.data.totalPayAmount");
        Number roi = JsonPath.read(json, "$.data.roi");
        assertTrue(payAmount.doubleValue() >= 100000);
        assertTrue(Math.abs(roi.doubleValue() - 4.0) < 0.01);
    }
}
