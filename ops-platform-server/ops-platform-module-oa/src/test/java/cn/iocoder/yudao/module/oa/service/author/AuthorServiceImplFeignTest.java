package cn.iocoder.yudao.module.oa.service.author;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.author.AuthorVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.MpAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.author.OaAuthorExtDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupAnchorRelDO;
import cn.iocoder.yudao.module.oa.service.account.MpAccountDataService;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.author.OaAuthorExtMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupAnchorRelMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.FollowerDailyMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.OpsAnchorRelMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorServiceImplFeignTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private OaAuthorExtMapper oaAuthorExtMapper;
    @Mock
    private MemberAuthorReadService memberAuthorReadService;
    @Mock
    private IpGroupMapper ipGroupMapper;
    @Mock
    private IpGroupAnchorRelMapper ipGroupAnchorRelMapper;
    @Mock
    private MpAccountDataService mpAccountDataService;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private OpsAnchorRelMapper opsAnchorRelMapper;
    @Mock
    private FollowerDailyMapper followerDailyMapper;
    @Mock
    private ContentMapper contentMapper;
    @Mock
    private AuthorResolveSupport authorResolveSupport;

    private AuthorServiceImpl authorService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        authorService = new AuthorServiceImpl(
                oaAuthorExtMapper,
                memberAuthorReadService,
                ipGroupMapper,
                ipGroupAnchorRelMapper,
                mpAccountDataService,
                sysUserMapper,
                opsAnchorRelMapper,
                followerDailyMapper,
                contentMapper,
                authorResolveSupport);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("G-MEM-02: list keyword 走 MemberAuthorReadService Feign")
    void listByKeywordViaFeign() {
        AuthorUserDO user = new AuthorUserDO();
        user.setId(1001L);
        user.setNickname("作者甲");
        user.setTenantId(TENANT_ID);
        user.setStatus(0);
        when(memberAuthorReadService.listByKeyword("作者", TENANT_ID)).thenReturn(List.of(user));

        OaAuthorExtDO ext = new OaAuthorExtDO();
        ext.setAuthorUserId(1001L);
        ext.setTenantId(TENANT_ID);
        ext.setStatus(1);
        when(oaAuthorExtMapper.selectList(any())).thenReturn(List.of(ext));
        when(authorResolveSupport.loadDisplayIpGroupIdByAuthor(eq(TENANT_ID), any())).thenReturn(Map.of());

        PageResult<AuthorVO> page = authorService.list(null, "作者", null, 1, 20);

        verify(memberAuthorReadService).listByKeyword("作者", TENANT_ID);
        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals("作者甲", page.getList().get(0).getNickname());
    }

    @Test
    @DisplayName("G-MEM-02: list ipGroupId 无绑定作者时返回空页")
    void listByIpGroupEmpty() {
        when(ipGroupAnchorRelMapper.selectList(any())).thenReturn(Collections.emptyList());

        PageResult<AuthorVO> page = authorService.list(99L, null, null, 1, 20);

        assertEquals(0L, page.getTotal());
        assertEquals(0, page.getList().size());
    }

    @Test
    @DisplayName("G-MEM-02: list ipGroupId 走 getAuthors 批量读")
    void listByIpGroupViaFeign() {
        IpGroupAnchorRelDO rel = new IpGroupAnchorRelDO();
        rel.setTenantId(TENANT_ID);
        rel.setIpGroupId(10L);
        rel.setAnchorUserId(1001L);
        when(ipGroupAnchorRelMapper.selectList(any())).thenReturn(List.of(rel));

        AuthorUserDO user = new AuthorUserDO();
        user.setId(1001L);
        user.setNickname("绑定作者");
        user.setTenantId(TENANT_ID);
        user.setStatus(0);
        when(memberAuthorReadService.loadByIds(anyCollection())).thenReturn(Map.of(1001L, user));

        OaAuthorExtDO ext = new OaAuthorExtDO();
        ext.setAuthorUserId(1001L);
        ext.setTenantId(TENANT_ID);
        ext.setStatus(1);
        when(oaAuthorExtMapper.selectList(any())).thenReturn(List.of(ext));

        PageResult<AuthorVO> page = authorService.list(10L, null, null, 1, 20);

        verify(memberAuthorReadService).loadByIds(anyCollection());
        assertEquals(1L, page.getTotal());
        assertEquals("绑定作者", page.getList().get(0).getNickname());
        assertEquals(10L, page.getList().get(0).getIpGroupId());
    }

    @Test
    @DisplayName("G-MP-01: list 主账号名称走 MpAccountDataService Feign")
    void listPrimaryAccountNameViaFeign() {
        AuthorUserDO user = new AuthorUserDO();
        user.setId(1001L);
        user.setNickname("作者甲");
        user.setTenantId(TENANT_ID);
        user.setStatus(0);
        when(memberAuthorReadService.listByKeyword("作者", TENANT_ID)).thenReturn(List.of(user));

        OaAuthorExtDO ext = new OaAuthorExtDO();
        ext.setAuthorUserId(1001L);
        ext.setTenantId(TENANT_ID);
        ext.setStatus(1);
        ext.setPrimaryMpAccountId(2001L);
        when(oaAuthorExtMapper.selectList(any())).thenReturn(List.of(ext));
        when(authorResolveSupport.loadDisplayIpGroupIdByAuthor(eq(TENANT_ID), any())).thenReturn(Map.of());

        MpAccountDO mp = new MpAccountDO();
        mp.setId(2001L);
        mp.setName("测试公众号");
        mp.setTenantId(TENANT_ID);
        when(mpAccountDataService.selectById(2001L)).thenReturn(mp);

        PageResult<AuthorVO> page = authorService.list(null, "作者", null, 1, 20);

        verify(mpAccountDataService).selectById(2001L);
        assertEquals("测试公众号", page.getList().get(0).getPrimaryAccountName());
    }
}
