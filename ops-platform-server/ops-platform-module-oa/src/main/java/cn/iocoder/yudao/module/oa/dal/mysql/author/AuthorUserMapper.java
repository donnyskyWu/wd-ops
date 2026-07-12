package cn.iocoder.yudao.module.oa.dal.mysql.author;

import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorUserDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("member")
public interface AuthorUserMapper extends BaseMapper<AuthorUserDO> {
}
