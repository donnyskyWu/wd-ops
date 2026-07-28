package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.framework.common.biz.mp.user.MpAccountInfoApi;
import cn.iocoder.yudao.framework.common.biz.mp.user.dto.MpAccountDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.MpAccountDO;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MpAccountDataServiceFeignDualRunTest {

    @Mock
    private MpAccountInfoApi mpAccountInfoApi;

    private MpAccountDataService service;

    @BeforeEach
    void setUp() {
        service = new MpAccountDataService(mpAccountInfoApi);
    }

    @Test
    @DisplayName("G-MP-01 cutover: selectById 走 getAccount Feign")
    void selectByIdPrefersFeign() {
        MpAccountDTO dto = new MpAccountDTO();
        dto.setId(501L);
        dto.setName("测试号");
        dto.setAppId("wx-test");
        when(mpAccountInfoApi.getAccount(501L)).thenReturn(CommonResult.success(dto));

        MpAccountDO mp = service.loadViaFeign(501L);

        assertEquals(501L, mp.getId());
        assertEquals("测试号", mp.getName());
    }

    @Test
    @DisplayName("G-MP-01 cutover: insert 走 createAccount Feign")
    void insertPrefersFeignCreate() {
        MpAccountDO mp = new MpAccountDO();
        mp.setName("新公众号");
        mp.setAppId("wx-new");
        when(mpAccountInfoApi.createAccount(any(MpAccountDTO.class))).thenReturn(CommonResult.success(601L));

        service.insert(mp);

        assertEquals(601L, mp.getId());
    }

    @Test
    @DisplayName("G-MP-01 cutover: insert Feign 失败时 fail-fast")
    void insertThrowsWhenFeignFails() {
        MpAccountDO mp = new MpAccountDO();
        mp.setName("新公众号");
        when(mpAccountInfoApi.createAccount(any())).thenThrow(new RuntimeException("mp-server down"));

        assertThrows(ServiceException.class, () -> service.insert(mp));
    }

    @Test
    @DisplayName("G-MP-01 cutover: selectPage 走 getAccountPage Feign")
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
    }

    @Test
    @DisplayName("G-MP-01 cutover: selectPage Feign 失败时 fail-fast")
    void selectPageThrowsWhenFeignFails() {
        when(mpAccountInfoApi.getAccountPage(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("mp-server down"));

        QueryWrapper<MpAccountDO> wrapper = new QueryWrapper<MpAccountDO>()
                .eq("tenant_id", 1L);

        assertThrows(ServiceException.class, () -> service.selectPage(new Page<>(1, 10), wrapper));
    }

    @Test
    @DisplayName("G-MP-01 cutover: selectPage wrapper 含 IN 时 fail-fast")
    void selectPageThrowsWhenWrapperHasInClause() {
        QueryWrapper<MpAccountDO> wrapper = new QueryWrapper<MpAccountDO>()
                .eq("tenant_id", 1L)
                .in("id", List.of(1L, 2L));

        assertThrows(ServiceException.class, () -> service.selectPage(new Page<>(1, 10), wrapper));
    }

    @Test
    @DisplayName("G-MP-01 cutover: getAccountPage 请求映射 name/status/page")
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
