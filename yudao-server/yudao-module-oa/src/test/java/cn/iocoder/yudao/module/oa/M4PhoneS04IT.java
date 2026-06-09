package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.phone.PhoneDO;
import cn.iocoder.yudao.module.oa.dal.mysql.phone.PhoneMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class M4PhoneS04IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PhoneMapper phoneMapper;

    @Test
    @DisplayName("S-04: 创建手机 + 列表脱敏")
    void createAndList() throws Exception {
        createPhone("13800138200", "PHONE-001");

        mockMvc.perform(get("/admin-api/oa/phone/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("phoneNumber", "13800138200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].phoneNumberMasked").value("138****8200"));
    }

    @Test
    @DisplayName("S-04: 手机号唯一 (2006)")
    void duplicatePhoneFails() throws Exception {
        createPhone("13800138201", "PHONE-A");
        mockMvc.perform(post("/admin-api/oa/phone/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "13800138201",
                                  "keeperId": 1001,
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2006));
    }

    @Test
    @DisplayName("S-04: 更新手机")
    void updatePhone() throws Exception {
        Long id = createPhone("13800138202", "PHONE-U");

        mockMvc.perform(put("/admin-api/oa/phone/update")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"id": %d, "phoneModel": "iPhone 16", "status": "ENABLED"}
                                """, id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("S-04: 被引用时不可删除 (1502)")
    void deleteBoundPhoneFails() throws Exception {
        Long id = createPhone("13800138203", "PHONE-D");
        PhoneDO entity = phoneMapper.selectById(id);
        entity.setAccountBoundCount(1);
        phoneMapper.updateById(entity);

        mockMvc.perform(delete("/admin-api/oa/phone/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(id)))
                .andExpect(jsonPath("$.code").value(1502));
    }

    @Test
    @DisplayName("S-04: 删除未绑定手机")
    void deletePhone() throws Exception {
        Long id = createPhone("13800138204", "PHONE-X");

        mockMvc.perform(delete("/admin-api/oa/phone/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(id)))
                .andExpect(jsonPath("$.code").value(0));
    }

    private Long createPhone(String phoneNumber, String phoneCode) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/oa/phone/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "phoneNumber": "%s",
                                  "phoneCode": "%s",
                                  "phoneModel": "Test Phone",
                                  "keeperId": 1001,
                                  "status": "ENABLED"
                                }
                                """, phoneNumber, phoneCode)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);
    }
}
