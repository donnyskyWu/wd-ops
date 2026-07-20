package cn.iocoder.yudao.module.oa.dal.mysql.account;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.MpUserDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("mp")
public interface MpUserMapper extends BaseMapper<MpUserDO> {
}
