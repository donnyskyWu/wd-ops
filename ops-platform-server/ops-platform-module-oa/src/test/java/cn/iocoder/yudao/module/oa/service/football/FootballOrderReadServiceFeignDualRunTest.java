package cn.iocoder.yudao.module.oa.service.football;

import cn.iocoder.yudao.framework.common.biz.pay.order.PayOrderApi;
import cn.iocoder.yudao.framework.common.biz.pay.order.dto.AllOrderRespDTO;
import cn.iocoder.yudao.framework.common.biz.pay.order.dto.OrderPageReqDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.football.FootballOrderListVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FootballOrderReadServiceFeignDualRunTest {

    @Mock
    private PayOrderApi payOrderApi;

    private FootballOrderReadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FootballOrderReadServiceImpl(payOrderApi);
    }

    @Test
    @DisplayName("G-PAY-01 cutover: Feign getOrderPage 成功时返回分页")
    void prefersFeignOrderPageWhenAvailable() {
        AllOrderRespDTO dto = new AllOrderRespDTO();
        dto.setId(70001L);
        dto.setOrderNo("P202607230001");
        dto.setUserId(3001L);
        dto.setAuthorId(1001L);
        dto.setAmount(new BigDecimal("88.00"));
        dto.setPayAmount(new BigDecimal("88.00"));
        dto.setStatus(1);
        dto.setOrderType(0);
        dto.setPayTime(LocalDateTime.of(2026, 7, 23, 10, 0));
        dto.setCreateTime(LocalDateTime.of(2026, 7, 23, 9, 59));
        when(payOrderApi.getOrderPage(any(OrderPageReqDTO.class)))
                .thenReturn(CommonResult.success(new PageResult<>(List.of(dto), 1L)));

        PageResult<FootballOrderListVO> page = service.loadPageViaFeign(
                LocalDate.of(2026, 7, 23).atStartOfDay(),
                LocalDate.of(2026, 7, 24).atStartOfDay(),
                null, null, 1, 20);

        assertEquals(1L, page.getTotal());
        assertEquals(70001L, page.getList().get(0).getId());
        assertEquals("P202607230001", page.getList().get(0).getOrderNo());
    }

    @Test
    @DisplayName("G-PAY-01 cutover: Feign 失败时 fail-fast")
    void throwsWhenFeignFails() {
        when(payOrderApi.getOrderPage(any())).thenThrow(new RuntimeException("pay-server down"));

        assertThrows(ServiceException.class, () -> service.loadPageViaFeign(
                LocalDate.of(2026, 7, 24).atStartOfDay(),
                LocalDate.of(2026, 7, 25).atStartOfDay(),
                null, null, 1, 20));
    }

    @Test
    @DisplayName("G-PAY-01 cutover: Feign 请求映射 createTime 与 authorId/status/page")
    void mapsOrderPageRequestFields() {
        when(payOrderApi.getOrderPage(any())).thenReturn(CommonResult.success(PageResult.empty()));

        LocalDateTime start = LocalDate.of(2026, 7, 1).atStartOfDay();
        LocalDateTime endExclusive = LocalDate.of(2026, 7, 2).atStartOfDay();
        service.loadPageViaFeign(start, endExclusive, 1001L, 1, 2, 10);

        ArgumentCaptor<OrderPageReqDTO> captor = ArgumentCaptor.forClass(OrderPageReqDTO.class);
        verify(payOrderApi).getOrderPage(captor.capture());
        OrderPageReqDTO req = captor.getValue();
        assertEquals(2, req.getPageNo());
        assertEquals(10, req.getPageSize());
        assertEquals(1001L, req.getAuthorId());
        assertEquals(1, req.getStatus());
        assertNotNull(req.getCreateTime());
        assertEquals(start, req.getCreateTime()[0]);
        assertEquals(endExclusive.minusNanos(1), req.getCreateTime()[1]);
    }
}
