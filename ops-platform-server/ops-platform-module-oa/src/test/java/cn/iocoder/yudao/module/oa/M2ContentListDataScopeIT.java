package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.mysql.content.ProductionContentMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 内容管理列表数据范围：管理员全量，普通用户仅本人关联内容。
 */
@AutoConfigureMockMvc
class M2ContentListDataScopeIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String OPERATOR = "Bearer dev-token-oa-operator";
    private static final String TENANT = "1";
    private static final String CONTENT_BASE = "/admin-api/oa/content";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductionContentMapper productionContentMapper;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    @DisplayName("内容列表：管理员可见他人创建的内容")
    void adminSeesAllTenantContent() throws Exception {
        long foreignContentId = insertForeignContent();

        int adminTotal = getListTotal(ADMIN);
        assertThat(adminTotal).isGreaterThan(0);

        mockMvc.perform(get(CONTENT_BASE + "/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("title", "IT-SCOPE-FOREIGN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(foreignContentId));

        productionContentMapper.deleteById(foreignContentId);
    }

    @Test
    @DisplayName("内容列表：运营专员不可见他人创建的内容")
    void operatorSeesOnlyOwnContent() throws Exception {
        long foreignContentId = insertForeignContent();

        mockMvc.perform(get(CONTENT_BASE + "/list")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .param("title", "IT-SCOPE-FOREIGN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));

        int operatorTotal = getListTotal(OPERATOR);
        int adminTotal = getListTotal(ADMIN);
        assertThat(operatorTotal).isLessThan(adminTotal);

        productionContentMapper.deleteById(foreignContentId);
    }

    private long insertForeignContent() {
        ProductionContentDO entity = new ProductionContentDO();
        entity.setTenantId(1L);
        entity.setTitle("IT-SCOPE-FOREIGN");
        entity.setBody("scope-it");
        entity.setBodyFormat("PLAIN");
        entity.setCreatorUserId(1001L);
        entity.setContentType("ARTICLE");
        entity.setDocumentType("MATCH_PREVIEW");
        entity.setStatus("DRAFT");
        entity.setAiGenerated(0);
        entity.setCreator("it-scope");
        entity.setUpdater("it-scope");
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        productionContentMapper.insert(entity);
        return entity.getId();
    }

    private int getListTotal(String token) throws Exception {
        MvcResult result = mockMvc.perform(get(CONTENT_BASE + "/list")
                        .header("Authorization", token)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNum", "1")
                        .param("pageSize", "200"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("total").asInt();
    }
}
