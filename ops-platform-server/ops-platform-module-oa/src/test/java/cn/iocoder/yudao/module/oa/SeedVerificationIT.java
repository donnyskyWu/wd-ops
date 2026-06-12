package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.company.CompanyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.SysDictDataDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.finance.AccountCostDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.monitor.ExternalWorkDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.realname.RealnameDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.OrderAttributionDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.OrderDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfRecordDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfTemplateDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.SopTemplateDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.TaskDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserTokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.author.AuthorMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.company.CompanyMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.dict.SysDictDataMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.content.ProductionContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.finance.AccountCostMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.monitor.ExternalWorkMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.realname.RealnameMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.FollowerDailyMapper;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.OrderMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfRecordMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfTemplateMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.OrderAttributionMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.SopTemplateMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.TaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedVerificationIT extends OaITBase {

    private static final long TENANT_1 = 1L;
    private static final long TENANT_2 = 2L;

    @Autowired
    private SysUserTokenMapper sysUserTokenMapper;

    @Autowired
    private SysDictDataMapper sysDictDataMapper;

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private RealnameMapper realnameMapper;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private IpGroupMapper ipGroupMapper;

    @Autowired
    private AuthorMapper authorMapper;

    @Autowired
    private FollowerDailyMapper followerDailyMapper;

    @Autowired
    private SopTemplateMapper sopTemplateMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ProductionContentMapper productionContentMapper;

    @Autowired
    private PerfTemplateMapper perfTemplateMapper;

    @Autowired
    private PerfRecordMapper perfRecordMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderAttributionMapper orderAttributionMapper;

    @Autowired
    private ContentMapper contentMapper;

    @Autowired
    private AccountCostMapper accountCostMapper;

    @Autowired
    private ExternalWorkMapper externalWorkMapper;

    @Test
    void seedBaseUsersExist() {
        assertTrue(sysUserTokenMapper.selectUserByToken("dev-token-oa-admin") != null);
        assertTrue(sysUserTokenMapper.selectUserByToken("dev-token-oa-tenantb") != null);
        assertTrue(sysUserTokenMapper.selectUserByToken("dev-token-oa-leader") != null);
        assertTrue(sysUserTokenMapper.selectUserByToken("dev-token-oa-operator") != null);
    }

    @Test
    @DisplayName("SEED-AUTH-001: 多角色用户与权限点")
    void seedAuth() {
        assertNotNull(sysUserTokenMapper.selectUserByToken("dev-token-oa-admin"));
        assertEquals(1003L, sysUserTokenMapper.selectUserByToken("dev-token-oa-operator").getId());
        assertTrue(sysUserTokenMapper.selectPermissionCodesByUserId(1001L).contains("oa:tenant:create"));
        assertTrue(sysUserTokenMapper.selectPermissionCodesByUserId(1003L).contains("oa:account:list"));
        assertTrue(!sysUserTokenMapper.selectPermissionCodesByUserId(1003L).contains("oa:tenant:create"));
    }

    @Test
    void seedBaseDictExists() {
        Long count = sysDictDataMapper.selectCount(new LambdaQueryWrapper<SysDictDataDO>()
                .eq(SysDictDataDO::getDictType, "dict_platform_type"));
        assertTrue(count != null && count >= 2);
    }

    @Test
    @DisplayName("SEED-ASSETS-001: tenant=1 公司≥2 实名人≥5 账号≥10")
    void seedAssets() {
        long companyCount = companyMapper.selectCount(new LambdaQueryWrapper<CompanyDO>()
                .eq(CompanyDO::getTenantId, TENANT_1)
                .likeRight(CompanyDO::getCompanyName, "SEED-"));
        long realnameCount = realnameMapper.selectCount(new LambdaQueryWrapper<RealnameDO>()
                .eq(RealnameDO::getTenantId, TENANT_1)
                .likeRight(RealnameDO::getRealName, "SEED-"));
        long accountCount = accountMapper.selectCount(new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, TENANT_1)
                .likeRight(AccountDO::getAccountName, "SEED-"));

        assertTrue(companyCount >= 2, "tenant=1 SEED 公司应 ≥ 2");
        assertTrue(realnameCount >= 5, "tenant=1 SEED 实名人应 ≥ 5");
        assertTrue(accountCount >= 10, "tenant=1 SEED 账号应 ≥ 10");

        CompanyDO companyA = companyMapper.selectById(9001L);
        assertNotNull(companyA);
        assertEquals("SEED-种子科技A", companyA.getCompanyName());
        assertEquals(TENANT_1, companyA.getTenantId());
    }

    @Test
    @DisplayName("SEED-ASSETS-002: tenant=2 隔离样本")
    void seedAssetsTenant2Isolation() {
        CompanyDO t2Company = companyMapper.selectOne(new LambdaQueryWrapper<CompanyDO>()
                .eq(CompanyDO::getTenantId, TENANT_2)
                .eq(CompanyDO::getCreditCode, "91110000MA0SEEDT02"));
        assertNotNull(t2Company, "tenant=2 隔离公司应存在");

        AccountDO t2Account = accountMapper.selectById(8001L);
        assertNotNull(t2Account);
        assertEquals(TENANT_2, t2Account.getTenantId());
        assertEquals("SEED-T2-公众号", t2Account.getAccountName());
    }

    @Test
    @DisplayName("SEED-OPS-001: tenant=1 IP组≥3 作者≥5 粉丝日表≥30")
    void seedOps() {
        long ipGroupCount = ipGroupMapper.selectCount(new LambdaQueryWrapper<IpGroupDO>()
                .eq(IpGroupDO::getTenantId, TENANT_1));
        long authorCount = authorMapper.selectCount(new LambdaQueryWrapper<AuthorDO>()
                .eq(AuthorDO::getTenantId, TENANT_1)
                .likeRight(AuthorDO::getAuthorName, "SEED-"));
        long followerDailyCount = followerDailyMapper.selectCount(new LambdaQueryWrapper<FollowerDailyDO>()
                .eq(FollowerDailyDO::getTenantId, TENANT_1)
                .eq(FollowerDailyDO::getAccountId, 9001L));

        assertTrue(ipGroupCount >= 3, "tenant=1 IP 组应 ≥ 3");
        assertTrue(authorCount >= 5, "tenant=1 SEED 作者应 ≥ 5");
        assertTrue(followerDailyCount >= 90, "账号 9001 粉丝日表应 ≥ 90");
    }

    @Test
    @DisplayName("SEED-CONTENT-001: tenant=1 SOP≥2 任务≥10 生产内容多状态")
    void seedContent() {
        long sopCount = sopTemplateMapper.selectCount(new LambdaQueryWrapper<SopTemplateDO>()
                .eq(SopTemplateDO::getTenantId, TENANT_1)
                .likeRight(SopTemplateDO::getTemplateName, "SEED-"));
        long taskCount = taskMapper.selectCount(new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getTenantId, TENANT_1)
                .likeRight(TaskDO::getPlanName, "SEED-"));
        long contentCount = productionContentMapper.selectCount(new LambdaQueryWrapper<ProductionContentDO>()
                .eq(ProductionContentDO::getTenantId, TENANT_1)
                .likeRight(ProductionContentDO::getTitle, "SEED-"));
        long draftCount = productionContentMapper.selectCount(new LambdaQueryWrapper<ProductionContentDO>()
                .eq(ProductionContentDO::getTenantId, TENANT_1)
                .eq(ProductionContentDO::getStatus, "DRAFT"));
        long publishedCount = productionContentMapper.selectCount(new LambdaQueryWrapper<ProductionContentDO>()
                .eq(ProductionContentDO::getTenantId, TENANT_1)
                .eq(ProductionContentDO::getStatus, "PUBLISHED"));

        assertTrue(sopCount >= 2, "tenant=1 SEED SOP 模板应 ≥ 2");
        assertTrue(taskCount >= 10, "tenant=1 SEED 任务应 ≥ 10");
        assertTrue(contentCount >= 5, "tenant=1 SEED 生产内容应 ≥ 5");
        assertTrue(draftCount >= 1, "应存在 DRAFT 状态内容");
        assertTrue(publishedCount >= 1, "应存在 PUBLISHED 状态内容");
    }

    @Test
    @DisplayName("SEED-PERF-001: tenant=1 模板≥2 记录≥5 订单≥10")
    void seedPerf() {
        long templateCount = perfTemplateMapper.selectCount(new LambdaQueryWrapper<PerfTemplateDO>()
                .eq(PerfTemplateDO::getTenantId, TENANT_1)
                .likeRight(PerfTemplateDO::getTemplateName, "SEED-"));
        long recordCount = perfRecordMapper.selectCount(new LambdaQueryWrapper<PerfRecordDO>()
                .eq(PerfRecordDO::getTenantId, TENANT_1));
        long draftCount = perfRecordMapper.selectCount(new LambdaQueryWrapper<PerfRecordDO>()
                .eq(PerfRecordDO::getTenantId, TENANT_1)
                .eq(PerfRecordDO::getStatus, "DRAFT"));
        long confirmedCount = perfRecordMapper.selectCount(new LambdaQueryWrapper<PerfRecordDO>()
                .eq(PerfRecordDO::getTenantId, TENANT_1)
                .eq(PerfRecordDO::getStatus, "CONFIRMED"));
        long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<OrderDO>()
                .eq(OrderDO::getTenantId, TENANT_1)
                .likeRight(OrderDO::getOrderNo, "SEED-ORD-"));
        long attrCount = orderAttributionMapper.selectCount(new LambdaQueryWrapper<OrderAttributionDO>()
                .eq(OrderAttributionDO::getTenantId, TENANT_1));

        assertTrue(templateCount >= 2, "tenant=1 SEED 绩效模板应 ≥ 2");
        assertTrue(recordCount >= 5, "tenant=1 考核记录应 ≥ 5");
        assertTrue(draftCount >= 1, "应存在 DRAFT 考核记录");
        assertTrue(confirmedCount >= 1, "应存在 CONFIRMED 考核记录");
        assertTrue(orderCount >= 10, "tenant=1 SEED 订单应 ≥ 10");
        assertTrue(attrCount >= 10, "tenant=1 订单归因应 ≥ 10");
    }

    @Test
    @DisplayName("SEED-ANALYTICS-001: 粉丝日表≥90 内容≥20")
    void seedAnalytics() {
        long follower9001 = followerDailyMapper.selectCount(new LambdaQueryWrapper<FollowerDailyDO>()
                .eq(FollowerDailyDO::getTenantId, TENANT_1)
                .eq(FollowerDailyDO::getAccountId, 9001L));
        long follower9002 = followerDailyMapper.selectCount(new LambdaQueryWrapper<FollowerDailyDO>()
                .eq(FollowerDailyDO::getTenantId, TENANT_1)
                .eq(FollowerDailyDO::getAccountId, 9002L));
        long contentCount = contentMapper.selectCount(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, TENANT_1)
                .likeRight(ContentDO::getTitle, "SEED-"));

        assertTrue(follower9001 >= 90, "账号 9001 粉丝日表应 ≥ 90");
        assertTrue(follower9002 >= 90, "账号 9002 粉丝日表应 ≥ 90");
        assertTrue(contentCount >= 20, "tenant=1 SEED 内容应 ≥ 20");
    }

    @Test
    @DisplayName("SEED-MONITOR-001: 成本≥10 外部作品≥15")
    void seedMonitor() {
        long costCount = accountCostMapper.selectCount(new LambdaQueryWrapper<AccountCostDO>()
                .eq(AccountCostDO::getTenantId, TENANT_1));
        long externalCount = externalWorkMapper.selectCount(new LambdaQueryWrapper<ExternalWorkDO>()
                .eq(ExternalWorkDO::getTenantId, TENANT_1)
                .eq(ExternalWorkDO::getIsExternal, 1));
        long hitCount = externalWorkMapper.selectCount(new LambdaQueryWrapper<ExternalWorkDO>()
                .eq(ExternalWorkDO::getTenantId, TENANT_1)
                .ge(ExternalWorkDO::getPlayCount, 1_000_000L));

        assertTrue(costCount >= 10, "tenant=1 账号成本应 ≥ 10");
        assertTrue(externalCount >= 15, "tenant=1 外部作品应 ≥ 15");
        assertTrue(hitCount >= 1, "应存在爆款外部作品");
    }
}
