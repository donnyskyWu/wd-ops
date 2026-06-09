package cn.iocoder.yudao.module.oa.dal.mysql.auth;

import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUserDO> {
}
