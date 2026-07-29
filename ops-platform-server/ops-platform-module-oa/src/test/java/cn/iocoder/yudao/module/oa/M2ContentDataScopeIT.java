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
 * 内容管理列表数据范围：IP 组长仅管辖组；非 admin 仅可见 creator_user_id=本人的内容（6117）。
 */
@AutoConfigureMockMvc
class M2ContentDataScopeIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String OPERATOR = "Bearer dev-token-oa-operator";
    private static final String LEADER = "Bearer dev-token-oa-leader";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductionContentMapper productionContentMapper;

    @BeforeEach
    void seedScopeFixtures() {
        seedIfAbsent("IT-管理员专属内容", content -> {
            content.setCreatorUserId(1001L);
            content.setAccountId(9006L);
            content.setIpGroupId(null);
        });
        seedIfAbsent("IT-组外内容", content -> {
            content.setCreatorUserId(1003L);
            content.setAccountId(9003L);
            content.setIpGroupId(9999L);
        });
        seedIfAbsent("IT-组内内容", content -> {
            content.setCreatorUserId(1003L);
            content.setAccountId(9001L);
            content.setIpGroupId(9001L);
        });
    }

    private void seedIfAbsent(String title, java.util.function.Consumer<ProductionContentDO> customizer) {
        Long count = productionContentMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProductionContentDO>()
                        .eq(ProductionContentDO::getTenantId, 1L)
                        .eq(ProductionContentDO::getTitle, title));
        if (count != null && count > 0) {
            return;
        }
        ProductionContentDO entity = new ProductionContentDO();
        entity.setTenantId(1L);
        entity.setTitle(title);
        entity.setBody("scope test");
        entity.setPlatformType("DOUYIN");
        entity.setContentType("SHORT_VIDEO");
        entity.setStatus("DRAFT");
        entity.setAiGenerated(0);
        entity.setCreator("it-scope");
        entity.setUpdater("it-scope");
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        customizer.accept(entity);
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
    @DisplayName("M2: IP 组长内容列表仅见管辖 IP 组")
    void ipGroupLeaderListScopedToLedGroups() throws Exception {
        mockMvc.perform(get("/admin-api/oa/content/list")
                        .header("Authorization", LEADER)
                        .header("X-Tenant-Id", TENANT)
                        .param("title", "IT-组内内容")
                        .param("pageSize", "10"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/admin-api/oa/content/list")
                        .header("Authorization", LEADER)
                        .header("X-Tenant-Id", TENANT)
                        .param("title", "IT-组外内容")
                        .param("pageSize", "10"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/admin-api/oa/content/list")
                        .header("Authorization", LEADER)
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
