package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.mysql.content.ProductionContentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Phase 3 数据权限：6117/6118 内容、6156 人效、6159 IP 组。
 */
@AutoConfigureMockMvc
class OpsDataScopePhase3IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String OPERATOR = "Bearer dev-token-oa-operator";
    private static final String LEADER = "Bearer dev-token-oa-leader";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductionContentMapper productionContentMapper;

    @BeforeEach
    void ensureReviewContentHasIpGroup() {
        ProductionContentDO pending = productionContentMapper.selectById(9432L);
        if (pending != null && pending.getIpGroupId() == null) {
            pending.setIpGroupId(9001L);
            productionContentMapper.updateById(pending);
        }
    }

    @Test
    @DisplayName("6156: 运营专员人效列表仅见本人")
    void operatorProductivityListSelfOnly() throws Exception {
        mockMvc.perform(get("/admin-api/oa/productivity-review/list")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .param("page", "1")
                        .param("size", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.userId == 1003)]").exists())
                .andExpect(jsonPath("$.data.list[?(@.userId == 1005)]").doesNotExist());
    }

    @Test
    @DisplayName("6156: 管理员人效列表可见多用户")
    void adminProductivityListSeesMultipleUsers() throws Exception {
        mockMvc.perform(get("/admin-api/oa/productivity-review/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("page", "1")
                        .param("size", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("6156: 运营专员不可查看他人人效详情")
    void operatorCannotViewOthersProductivityDetail() throws Exception {
        mockMvc.perform(get("/admin-api/oa/productivity-review/detail/1005")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("6159: 运营专员不可访问 IP 组树与列表")
    void operatorForbiddenOnIpGroupTreeAndList() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/tree")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(get("/admin-api/oa/ip-group/list")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("6159: 管理员可访问 IP 组树")
    void adminCanAccessIpGroupTree() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/tree")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("6118: 运营专员无一级审核队列权限")
    void operatorCannotAccessFirstReviewQueue() throws Exception {
        mockMvc.perform(get("/admin-api/oa/content/list")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .param("status", "PENDING_FIRST_REVIEW")
                        .param("pageSize", "20"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("6118: IP 组长/管理员可见可审待初审内容")
    void leaderSeesReviewablePendingFirstReview() throws Exception {
        mockMvc.perform(get("/admin-api/oa/content/list")
                        .header("Authorization", LEADER)
                        .header("X-Tenant-Id", TENANT)
                        .param("status", "PENDING_FIRST_REVIEW")
                        .param("pageSize", "20"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.id == 9432)]").exists());
    }

    @Test
    @DisplayName("6117: 运营专员内容列表不含他人创建")
    void operatorContentListSelfCreatedOnly() throws Exception {
        Long othersCount = productionContentMapper.selectCount(
                new LambdaQueryWrapper<ProductionContentDO>()
                        .eq(ProductionContentDO::getTenantId, 1L)
                        .ne(ProductionContentDO::getCreatorUserId, 1003L));
        org.junit.jupiter.api.Assumptions.assumeTrue(othersCount != null && othersCount > 0);

        mockMvc.perform(get("/admin-api/oa/content/list")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.creatorUserId != 1003)]").doesNotExist());
    }
}
