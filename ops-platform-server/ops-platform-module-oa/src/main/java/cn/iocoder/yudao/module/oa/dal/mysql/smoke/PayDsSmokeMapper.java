package cn.iocoder.yudao.module.oa.dal.mysql.smoke;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
@DS("pay")
public interface PayDsSmokeMapper {

    @Select("SELECT 1")
    Integer selectOne();
}
