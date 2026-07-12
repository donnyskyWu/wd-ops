package cn.iocoder.yudao.module.oa.dal.mysql.account;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.OaAccountExtDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("master")
public interface OaAccountExtMapper extends BaseMapper<OaAccountExtDO> {
}
