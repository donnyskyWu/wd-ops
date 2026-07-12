package cn.iocoder.yudao.module.oa.dal.mysql.dict;

import cn.iocoder.yudao.module.oa.dal.dataobject.dict.FootballSystemDictDataDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
@DS("system")
public interface FootballSystemDictDataMapper extends BaseMapper<FootballSystemDictDataDO> {

    @Select("SELECT COUNT(*) FROM system_dict_data WHERE deleted = 0")
    Long countActiveRows();
}
