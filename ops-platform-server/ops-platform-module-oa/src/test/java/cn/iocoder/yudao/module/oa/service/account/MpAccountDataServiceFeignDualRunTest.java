package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.framework.common.biz.mp.user.MpAccountInfoApi;
import cn.iocoder.yudao.framework.common.biz.mp.user.dto.MpAccountDTO;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.MpAccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.MpAccountMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MpAccountDataServiceFeignDualRunTest {

    @Mock
    private MpAccountMapper mpAccountMapper;
    @Mock
    private MpAccountInfoApi mpAccountInfoApi;

    private MpAccountDataService service;

    @BeforeEach
    void setUp() {
        service = new MpAccountDataService(mpAccountMapper, mpAccountInfoApi);
    }

    @Test
    @DisplayName("G-MP-01: selectById 优先 getAccount Feign")
    void selectByIdPrefersFeign() {
        MpAccountDTO dto = new MpAccountDTO();
        dto.setId(501L);
        dto.setName("测试号");
        dto.setAppId("wx-test");
        when(mpAccountInfoApi.getAccount(501L)).thenReturn(CommonResult.success(dto));

        MpAccountDO mp = service.loadViaFeign(501L);

        assertEquals(501L, mp.getId());
        assertEquals("测试号", mp.getName());
        verify(mpAccountMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("G-MP-01: insert 优先 createAccount Feign")
    void insertPrefersFeignCreate() {
        MpAccountDO mp = new MpAccountDO();
        mp.setName("新公众号");
        mp.setAppId("wx-new");
        when(mpAccountInfoApi.createAccount(any(MpAccountDTO.class))).thenReturn(CommonResult.success(601L));

        service.insert(mp);

        assertEquals(601L, mp.getId());
        verify(mpAccountMapper, never()).insert(any(MpAccountDO.class));
    }

    @Test
    @DisplayName("G-MP-01: insert Feign 失败回退 @DS")
    void insertFallsBackToDsWhenFeignFails() {
        MpAccountDO mp = new MpAccountDO();
        mp.setName("新公众号");
        when(mpAccountInfoApi.createAccount(any())).thenThrow(new RuntimeException("mp-server down"));

        service.insert(mp);

        verify(mpAccountMapper).insert(mp);
    }

    @Test
    @DisplayName("G-MP-01: selectPage 优先 getAccountPage Feign")
    void selectPagePrefersFeign() {
        MpAccountDTO dto = new MpAccountDTO();
        dto.setId(701L);
        dto.setName("分页号");
        dto.setAppId("wx-page");
        when(mpAccountInfoApi.getAccountPage(eq(1), eq(10), eq("测试"), eq(null), eq(null), eq(0), eq(null)))
                .thenReturn(CommonResult.success(new PageResult<>(List.of(dto), 1L)));

        QueryWrapper<MpAccountDO> wrapper = new QueryWrapper<MpAccountDO>()
                .eq("tenant_id", 1L)
                .like("name", "测试")
                .eq("status", 0);
        Page<MpAccountDO> page = service.selectPage(new Page<>(1, 10), wrapper);

        assertEquals(1L, page.getTotal());
        assertEquals(701L, page.getRecords().get(0).getId());
        verify(mpAccountMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("G-MP-01: selectPage Feign 失败回退 @DS")
    void selectPageFallsBackToDsWhenFeignFails() {
        when(mpAccountInfoApi.getAccountPage(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("mp-server down"));
        Page<MpAccountDO> dsPage = new Page<>(1, 10);
        dsPage.setRecords(List.of());
        dsPage.setTotal(0L);
        when(mpAccountMapper.selectPage(any(), any())).thenReturn(dsPage);

        QueryWrapper<MpAccountDO> wrapper = new QueryWrapper<MpAccountDO>()
                .eq("tenant_id", 1L);
        Page<MpAccountDO> page = service.selectPage(new Page<>(1, 10), wrapper);

        assertEquals(0L, page.getTotal());
        verify(mpAccountMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("G-MP-01: selectPage wrapper 含 IN 时跳过 Feign")
    void selectPageSkipsFeignWhenWrapperHasInClause() {
        Page<MpAccountDO> dsPage = new Page<>(1, 10);
        dsPage.setRecords(List.of());
        dsPage.setTotal(0L);
        when(mpAccountMapper.selectPage(any(), any())).thenReturn(dsPage);

        QueryWrapper<MpAccountDO> wrapper = new QueryWrapper<MpAccountDO>()
                .eq("tenant_id", 1L)
                .in("id", List.of(1L, 2L));
        service.selectPage(new Page<>(1, 10), wrapper);

        verify(mpAccountInfoApi, never()).getAccountPage(any(), any(), any(), any(), any(), any(), any());
        verify(mpAccountMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("G-MP-01: getAccountPage 请求映射 name/status/page")
    void mapsAccountPageRequestFields() {
        when(mpAccountInfoApi.getAccountPage(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(CommonResult.success(PageResult.empty()));

        QueryWrapper<MpAccountDO> wrapper = new QueryWrapper<MpAccountDO>()
                .eq("tenant_id", 1L)
                .like("name", "足球")
                .eq("status", 1);
        service.loadPageViaFeign(new Page<>(2, 20), wrapper);

        ArgumentCaptor<Integer> pageNoCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mpAccountInfoApi).getAccountPage(
                pageNoCaptor.capture(), eq(20), eq("足球"), eq(null), eq(null), eq(1), eq(null));
        assertEquals(2, pageNoCaptor.getValue());
    }
}
