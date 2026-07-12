package cn.iocoder.yudao.module.oa.dal.mysql.system;

import cn.iocoder.yudao.module.oa.dal.dataobject.system.FootballSystemLoginLogDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
@DS("system")
public interface FootballSystemLoginLogMapper extends BaseMapper<FootballSystemLoginLogDO> {

    @Select("SELECT COUNT(*) FROM system_login_log WHERE deleted = 0")
    Long countActiveRows();
}
