package cn.iocoder.yudao.module.oa.service.system;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.system.PermissionRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.RoleAssignPermissionReq;
import cn.iocoder.yudao.module.oa.api.dto.system.RoleCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.RoleRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.RoleUpdateReq;

import java.util.List;

public interface RoleService {

    PageResult<RoleRespVO> list(String name, String code, Integer pageNo, Integer pageSize);

    Long create(RoleCreateReq req);

    void update(RoleUpdateReq req);

    void delete(Long id);

    void assignPermission(RoleAssignPermissionReq req);

    List<PermissionRespVO> listPermissions(Long roleId);
}
