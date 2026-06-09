package cn.iocoder.yudao.module.oa.dal.mysql.author;

import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthorMapper extends BaseMapper<AuthorDO> {
}
