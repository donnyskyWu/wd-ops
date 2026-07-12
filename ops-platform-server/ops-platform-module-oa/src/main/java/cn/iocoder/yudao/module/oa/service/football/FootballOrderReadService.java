package cn.iocoder.yudao.module.oa.service.football;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.football.FootballOrderListVO;

import java.time.LocalDate;

public interface FootballOrderReadService {

    PageResult<FootballOrderListVO> listPayAllOrders(LocalDate startDate, LocalDate endDate,
                                                     Long authorId, Integer status,
                                                     Integer pageNum, Integer pageSize);
}
