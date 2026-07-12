package cn.iocoder.yudao.module.oa.dal.mysql.system;

import cn.iocoder.yudao.module.oa.dal.dataobject.system.FootballSystemOperateLogDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
@DS("system")
public interface FootballSystemOperateLogMapper extends BaseMapper<FootballSystemOperateLogDO> {

    @Select("SELECT COUNT(*) FROM system_operate_log WHERE deleted = 0")
    Long countActiveRows();
}
