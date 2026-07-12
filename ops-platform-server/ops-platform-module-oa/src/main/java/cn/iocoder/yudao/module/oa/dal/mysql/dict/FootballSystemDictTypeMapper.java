package cn.iocoder.yudao.module.oa.dal.mysql.dict;

import cn.iocoder.yudao.module.oa.dal.dataobject.dict.FootballSystemDictTypeDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("system")
public interface FootballSystemDictTypeMapper extends BaseMapper<FootballSystemDictTypeDO> {
}
