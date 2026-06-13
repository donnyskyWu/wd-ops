package cn.iocoder.yudao.module.oa.service.user;

import cn.iocoder.yudao.module.oa.api.dto.user.UserIpGroupVO;

import java.util.List;

public interface UserContentContextService {

    /**
     * 当前用户所属 IP 组及组内作者（默认取第一个 IP 组）
     */
    List<UserIpGroupVO> listMyIpGroups();
}
