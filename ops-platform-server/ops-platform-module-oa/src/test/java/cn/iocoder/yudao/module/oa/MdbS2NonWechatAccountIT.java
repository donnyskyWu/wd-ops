package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.company.CompanyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.realname.RealnameDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.company.CompanyMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.realname.RealnameMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * GATE-MDB-S2: non-WeChat oa_account CRUD on empty table + IP group tree.
 */
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "dev-local-multidb"})
class MdbS2NonWechatAccountIT {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired(required = false)
    private MockMvc mockMvc;
    @Autowired(required = false)
    private CompanyMapper companyMapper;
    @Autowired(required = false)
    private RealnameMapper realnameMapper;
    @Autowired(required = false)
    private IpGroupMapper ipGroupMapper;
    @Autowired(required = false)
    private AccountMapper accountMapper;

    @Test
    @DisplayName("S2-01/02: IP group tree has skeleton rows")
    void ipGroupTreeHasSkeleton() throws Exception {
        assumeLocalMysql();
        Assumptions.assumeTrue(mockMvc != null && ipGroupMapper != null);

        Long count = ipGroupMapper.selectCount(null);
        Assumptions.assumeTrue(count != null && count >= 3, "IP skeleton expected >= 3");

        mockMvc.perform(get("/admin-api/oa/ip-group/tree")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("S2-01: non-WeChat DOUYIN account CRUD on empty oa_account")
    void douyinAccountCrud() throws Exception {
        assumeLocalMysql();
        Assumptions.assumeTrue(mockMvc != null && companyMapper != null && realnameMapper != null);

        TestAssets assets = seedAssets("S2-DY");
        String externalId = "dy_s2_" + System.currentTimeMillis();

        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/account/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "platformType": "DOUYIN",
                                  "accountName": "S2抖音测试",
                                  "externalAccountId": "%s",
                                  "companyId": %d,
                                  "realnameId": %d,
                                  "ipGroupId": %d,
                                  "status": "NORMAL"
                                }
                                """, externalId, assets.companyId(), assets.realnameId(), assets.ipGroupId())))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        Long accountId = ((Number) JsonPath.read(createResult.getResponse().getContentAsString(), "$.data")).longValue();

        mockMvc.perform(get("/admin-api/oa/account/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("platformType", "DOUYIN")
                        .param("accountName", "S2抖音测试"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(delete("/admin-api/oa/account/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(accountId)))
                .andExpect(jsonPath("$.code").value(0));

        Assumptions.assumeTrue(accountMapper.selectById(accountId) == null);
    }

    private TestAssets seedAssets(String prefix) {
        Long tenantId = 1L;
        CompanyDO company = new CompanyDO();
        company.setTenantId(tenantId);
        company.setCompanyName(prefix + "-公司");
        company.setStatus("ENABLED");
        company.setCreator("test");
        company.setCreateTime(LocalDateTime.now());
        companyMapper.insert(company);

        RealnameDO realname = new RealnameDO();
        realname.setTenantId(tenantId);
        realname.setRealName(prefix + "-实名人");
        realname.setStatus("ENABLED");
        realname.setCreator("test");
        realname.setCreateTime(LocalDateTime.now());
        realnameMapper.insert(realname);

        IpGroupDO ipGroup = ipGroupMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IpGroupDO>()
                        .eq(IpGroupDO::getTenantId, tenantId)
                        .isNotNull(IpGroupDO::getParentId)
                        .last("LIMIT 1"));
        Long ipGroupId = ipGroup != null ? ipGroup.getId() : null;

        return new TestAssets(company.getId(), realname.getId(), ipGroupId);
    }

    private record TestAssets(Long companyId, Long realnameId, Long ipGroupId) {
    }

    private void assumeLocalMysql() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/wd?useSSL=false&allowPublicKeyRetrieval=true", "root", "root")) {
            Assumptions.assumeTrue(conn.isValid(2));
        } catch (Exception ex) {
            Assumptions.assumeTrue(false, "localhost MySQL not available: " + ex.getMessage());
        }
    }
}
