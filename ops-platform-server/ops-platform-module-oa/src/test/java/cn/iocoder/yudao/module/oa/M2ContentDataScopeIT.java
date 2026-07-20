package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.mysql.content.ProductionContentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 内容管理列表数据范围：非 admin 仅可见 creator_user_id=本人的内容（6117 P1 收窄）。
 */
@AutoConfigureMockMvc
class M2ContentDataScopeIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String OPERATOR = "Bearer dev-token-oa-operator";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductionContentMapper productionContentMapper;

    @BeforeEach
    void seedAdminOwnedContent() {
        Long count = productionContentMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProductionContentDO>()
                        .eq(ProductionContentDO::getTenantId, 1L)
                        .eq(ProductionContentDO::getTitle, "IT-管理员专属内容"));
        if (count != null && count > 0) {
            return;
        }
        ProductionContentDO entity = new ProductionContentDO();
        entity.setTenantId(1L);
        entity.setTitle("IT-管理员专属内容");
        entity.setBody("scope test");
        entity.setCreatorUserId(1001L);
        entity.setAccountId(9006L);
        entity.setPlatformType("DOUYIN");
        entity.setContentType("SHORT_VIDEO");
        entity.setStatus("DRAFT");
        entity.setAiGenerated(0);
        entity.setCreator("it-scope");
        entity.setUpdater("it-scope");
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        productionContentMapper.insert(entity);
    }

    @Test
    @DisplayName("M2: 管理员可见全部 SEED 内容")
    void adminSeesAllSeedContent() throws Exception {
        mockMvc.perform(get("/admin-api/oa/content/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(5)));
    }

    @Test
    @DisplayName("M2: 运营专员列表不含他人创建的内容")
    void operatorListExcludesOthersContent() throws Exception {
        mockMvc.perform(get("/admin-api/oa/content/list")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .param("title", "IT-管理员专属内容")
                        .param("pageSize", "10"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("M2: 运营专员不可查看他人内容详情")
    void operatorCannotReadOthersContent() throws Exception {
        ProductionContentDO entity = productionContentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProductionContentDO>()
                        .eq(ProductionContentDO::getTenantId, 1L)
                        .eq(ProductionContentDO::getTitle, "IT-管理员专属内容")
                        .last("LIMIT 1"));

        mockMvc.perform(get("/admin-api/oa/content/" + entity.getId())
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/content/" + entity.getId())
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(403));
    }
}
