package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.realname.RealnameDO;
import cn.iocoder.yudao.module.oa.dal.mysql.realname.RealnameMapper;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class M4RealnameS02IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RealnameMapper realnameMapper;

    @Autowired
    private AesUtil aesUtil;

    @Test
    @DisplayName("S-02: 创建实名人 + AES 加密 + 列表脱敏")
    void createListWithEncryption() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/realname/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "realName": "王联调",
                                  "idType": "ID_CARD",
                                  "idCard": "330101199001011234",
                                  "phone": "13800138001",
                                  "wechat": "wang_ld",
                                  "gender": "MALE",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        Long id = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);
        RealnameDO stored = realnameMapper.selectById(id);
        assertTrue(stored.getIdCardEncrypted().length() > 10);
        assertNotEquals("330101199001011234", stored.getIdCardEncrypted());
        assertNotEquals("13800138001", stored.getPhoneEncrypted());

        mockMvc.perform(get("/admin-api/oa/realname/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("realName", "王联调"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].idCardMasked").value("330101********1234"))
                .andExpect(jsonPath("$.data.list[0].phoneMasked").value("138****8001"));
    }

    @Test
    @DisplayName("S-02: 更新实名人")
    void updateRealname() throws Exception {
        Long id = createRealname("330101199102025678", "13900139001", "更新测试");

        mockMvc.perform(put("/admin-api/oa/realname/update")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"id": %d, "realName": "更新后姓名", "status": "ENABLED"}
                                """, id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("S-02: 删除未绑定实名人")
    void deleteRealname() throws Exception {
        Long id = createRealname("330101199203036789", "13700137001", "删除测试");

        mockMvc.perform(delete("/admin-api/oa/realname/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("S-02: 被引用时不可删除 (1502)")
    void deleteBoundRealnameFails() throws Exception {
        Long id = createRealname("330101199304047890", "13600136001", "绑定测试");
        RealnameDO entity = realnameMapper.selectById(id);
        entity.setAccountBoundCount(1);
        realnameMapper.updateById(entity);

        mockMvc.perform(delete("/admin-api/oa/realname/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1502));
    }

    private Long createRealname(String idCard, String phone, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/oa/realname/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "realName": "%s",
                                  "idType": "ID_CARD",
                                  "idCard": "%s",
                                  "phone": "%s",
                                  "status": "ENABLED"
                                }
                                """, name, idCard, phone)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);
    }
}
