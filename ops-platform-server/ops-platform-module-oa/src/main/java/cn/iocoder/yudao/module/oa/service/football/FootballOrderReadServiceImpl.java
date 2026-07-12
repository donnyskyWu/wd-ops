package cn.iocoder.yudao.module.oa.service.football;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.football.FootballOrderListVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.football.FootballPayAllOrderReadDO;
import cn.iocoder.yudao.module.oa.dal.mysql.football.FootballPayAllOrderReadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FootballOrderReadServiceImpl implements FootballOrderReadService {

    private final FootballPayAllOrderReadMapper footballPayAllOrderReadMapper;

    @Override
    public PageResult<FootballOrderListVO> listPayAllOrders(LocalDate startDate, LocalDate endDate,
                                                            Long authorId, Integer status,
                                                            Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        requireDateRange(startDate, endDate);
        int page = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();

        long total = footballPayAllOrderReadMapper.countPage(tenantId, startTime, endTime, authorId, status);
        if (total == 0) {
            return PageResult.empty();
        }
        List<FootballOrderListVO> rows = footballPayAllOrderReadMapper
                .selectPage(tenantId, startTime, endTime, authorId, status, (page - 1) * size, size)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(rows, total);
    }

    private FootballOrderListVO toVO(FootballPayAllOrderReadDO row) {
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
}
