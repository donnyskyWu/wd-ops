package cn.iocoder.yudao.module.oa.service.system;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.system.UserCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.UserRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.UserUpdateReq;

public interface UserService {

    PageResult<UserRespVO> list(String username, String nickname, Long roleId, String status,
                                Integer pageNo, Integer pageSize);

    Long create(UserCreateReq req);

    void update(UserUpdateReq req);

    void delete(Long id);
}
