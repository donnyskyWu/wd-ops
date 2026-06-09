package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M5FinanceCostS01IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M5-S-01: 创建成本记录")
    void createCost() throws Exception {
        mockMvc.perform(post("/admin-api/oa/finance/cost/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": 9001,
                                  "costType": "PURCHASE",
                                  "amount": 1000.00,
                                  "payMethod": "WECHAT",
                                  "payDate": "2026-06-07",
                                  "period": "ONCE",
                                  "remark": "IT-测试成本"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M5-S-01: 金额非法 (2015)")
    void invalidAmount() throws Exception {
        mockMvc.perform(post("/admin-api/oa/finance/cost/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": 9001,
                                  "costType": "PURCHASE",
                                  "amount": 0,
                                  "payMethod": "WECHAT",
                                  "payDate": "2026-06-07",
                                  "period": "ONCE"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(2015));
    }

    @Test
    @DisplayName("M5-S-01: SEED 成本列表≥10")
    void listCosts() throws Exception {
        mockMvc.perform(get("/admin-api/oa/finance/cost/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(10)));
    }
}
