package cn.iocoder.yudao.module.oa.dal.mysql.author;

import cn.iocoder.yudao.module.oa.dal.dataobject.author.OaAuthorExtDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("master")
public interface OaAuthorExtMapper extends BaseMapper<OaAuthorExtDO> {
}
