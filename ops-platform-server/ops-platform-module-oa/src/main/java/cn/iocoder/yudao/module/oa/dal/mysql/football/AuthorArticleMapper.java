package cn.iocoder.yudao.module.oa.dal.mysql.football;

import cn.iocoder.yudao.module.oa.dal.dataobject.football.AuthorArticleDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("member")
public interface AuthorArticleMapper extends BaseMapper<AuthorArticleDO> {
}
