package cn.iocoder.yudao.module.oa.service.system;

import cn.iocoder.yudao.module.oa.api.dto.system.PermissionRespVO;

import java.util.List;

public interface PermissionService {

    List<PermissionRespVO> listAll();
}
