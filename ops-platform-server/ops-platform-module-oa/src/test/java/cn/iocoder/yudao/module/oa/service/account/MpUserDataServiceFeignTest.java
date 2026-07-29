package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.framework.common.biz.mp.user.MpUserApi;
import cn.iocoder.yudao.framework.common.biz.mp.user.dto.MpUserDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.MpUserDO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MpUserDataServiceFeignTest {

    private static final Long TENANT_ID = 1L;
    private static final Long MP_ACCOUNT_ID = 501L;

    @Mock
    private MpUserApi mpUserApi;

    private MpUserDataService service;

    @BeforeEach
    void setUp() {
        service = new MpUserDataService(mpUserApi);
    }

    @Test
    @DisplayName("G-MP-01 cutover: selectPageByAccount 走 getUserPageByAccount Feign")
    void selectPageByAccountPrefersFeign() {
        MpUserDTO dto = new MpUserDTO();
        dto.setId(1001L);
        dto.setOpenid("oTest001");
        dto.setNickname("粉丝A");
        dto.setHeadImageUrl("https://example.com/a.png");
        dto.setSubscribeStatus(1);
        dto.setSubscribeTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        dto.setUpdateTime(LocalDateTime.of(2026, 6, 24, 8, 0));
        dto.setAccountId(MP_ACCOUNT_ID);
        when(mpUserApi.getUserPageByAccount(eq(MP_ACCOUNT_ID), eq(1), eq(10), eq(1)))
                .thenReturn(CommonResult.success(new PageResult<>(List.of(dto), 1L)));

        Page<MpUserDO> page = service.selectPageByAccount(new Page<>(1, 10), TENANT_ID, MP_ACCOUNT_ID);

        assertEquals(1L, page.getTotal());
        assertEquals(1001L, page.getRecords().get(0).getId());
        assertEquals("粉丝A", page.getRecords().get(0).getNickname());
        assertEquals(TENANT_ID, page.getRecords().get(0).getTenantId());
    }

    @Test
    @DisplayName("G-MP-01 cutover: Feign 失败时 fail-fast")
    void selectPageByAccountThrowsWhenFeignFails() {
        when(mpUserApi.getUserPageByAccount(eq(MP_ACCOUNT_ID), eq(1), eq(10), eq(1)))
                .thenThrow(new RuntimeException("mp-server down"));

        assertThrows(ServiceException.class,
                () -> service.selectPageByAccount(new Page<>(1, 10), TENANT_ID, MP_ACCOUNT_ID));
    }

    @Test
    @DisplayName("G-MP-01 cutover: getUserPageByAccount 请求映射 accountId/page/subscribeStatus")
    void mapsUserPageRequestFields() {
        when(mpUserApi.getUserPageByAccount(eq(MP_ACCOUNT_ID), eq(2), eq(20), eq(1)))
                .thenReturn(CommonResult.success(PageResult.empty()));

        service.loadPageViaFeign(new Page<>(2, 20), TENANT_ID, MP_ACCOUNT_ID);

        ArgumentCaptor<Integer> pageNoCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mpUserApi).getUserPageByAccount(eq(MP_ACCOUNT_ID), pageNoCaptor.capture(), eq(20), eq(1));
        assertEquals(2, pageNoCaptor.getValue());
    }

    @Test
    @DisplayName("G-MP-01 cutover: toDo 映射 DTO 字段")
    void toDoMapsFields() {
        MpUserDTO dto = new MpUserDTO();
        dto.setId(2001L);
        dto.setOpenid("oMap");
        dto.setNickname("映射粉丝");
        dto.setAccountId(MP_ACCOUNT_ID);

        MpUserDO user = MpUserDataService.toDo(dto, TENANT_ID);

        assertEquals(2001L, user.getId());
        assertEquals("oMap", user.getOpenid());
        assertEquals(TENANT_ID, user.getTenantId());
    }
}
