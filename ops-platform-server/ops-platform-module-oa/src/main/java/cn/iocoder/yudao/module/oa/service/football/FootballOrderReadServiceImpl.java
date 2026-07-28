package cn.iocoder.yudao.module.oa.service.football;

import cn.iocoder.yudao.framework.common.biz.pay.order.PayOrderApi;
import cn.iocoder.yudao.framework.common.biz.pay.order.dto.AllOrderRespDTO;
import cn.iocoder.yudao.framework.common.biz.pay.order.dto.OrderPageReqDTO;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.football.FootballOrderListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FootballOrderReadServiceImpl implements FootballOrderReadService {

    private final PayOrderApi payOrderApi;

    @Override
    public PageResult<FootballOrderListVO> listPayAllOrders(LocalDate startDate, LocalDate endDate,
                                                            Long authorId, Integer status,
                                                            Integer pageNum, Integer pageSize) {
        requireTenantId();
        requireDateRange(startDate, endDate);
        int page = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();
        return loadPageViaFeign(startTime, endTime, authorId, status, page, size);
    }

    /**
     * G-PAY-01 cutover: Feign {@link PayOrderApi#getOrderPage} only.
     */
    PageResult<FootballOrderListVO> loadPageViaFeign(LocalDateTime startTime, LocalDateTime endTime,
                                                     Long authorId, Integer status,
                                                     int page, int size) {
        if (payOrderApi == null) {
            throw rpcUnavailable();
        }
        try {
            OrderPageReqDTO req = new OrderPageReqDTO();
            req.setPageNo(page);
            req.setPageSize(size);
            req.setAuthorId(authorId);
            req.setStatus(status);
            LocalDateTime inclusiveEnd = endTime.minusNanos(1);
            req.setCreateTime(new LocalDateTime[]{startTime, inclusiveEnd});
            CommonResult<PageResult<AllOrderRespDTO>> result = payOrderApi.getOrderPage(req);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                throw rpcUnavailable();
            }
            PageResult<AllOrderRespDTO> data = result.getData();
            List<FootballOrderListVO> rows = data.getList() == null
                    ? List.of()
                    : data.getList().stream().map(this::toVO).collect(Collectors.toList());
            long total = data.getTotal() == null ? rows.size() : data.getTotal();
            return new PageResult<>(rows, total);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw rpcUnavailable();
        }
    }

    private FootballOrderListVO toVO(AllOrderRespDTO row) {
        FootballOrderListVO vo = new FootballOrderListVO();
        vo.setId(row.getId());
        vo.setOrderNo(row.getOrderNo());
        vo.setUserId(row.getUserId());
        vo.setAuthorId(row.getAuthorId());
        vo.setAmount(row.getAmount());
        vo.setPayAmount(row.getPayAmount());
        vo.setStatus(row.getStatus());
        vo.setOrderType(row.getOrderType());
        vo.setPayTime(row.getPayTime());
        vo.setCreateTime(row.getCreateTime());
        return vo;
    }

    private void requireDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID.getCode(), "startDate 与 endDate 必填");
        }
        if (endDate.isBefore(startDate)) {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID.getCode(), "endDate 不能早于 startDate");
        }
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }

    private static ServiceException rpcUnavailable() {
        return new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                "订单查询服务不可用，请确认 pay-server 已启动");
    }
}
