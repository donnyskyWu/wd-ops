package cn.iocoder.yudao.module.oa.dal.mysql.perf;

import cn.iocoder.yudao.module.oa.dal.dataobject.perf.OrderDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<OrderDO> {
}
