package cn.iocoder.yudao.module.oa.service.system;

import cn.iocoder.yudao.module.oa.api.dto.system.DeptCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.DeptTreeVO;
import cn.iocoder.yudao.module.oa.api.dto.system.DeptUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.DingTalkSyncResultVO;

import java.util.List;

public interface DeptService {

    List<DeptTreeVO> getTree();

    Long create(DeptCreateReq req);

    void update(DeptUpdateReq req);

    void delete(Long id);

    DingTalkSyncResultVO syncDepartmentsFromDingTalk();

    DingTalkSyncResultVO syncUsersFromDingTalk();
}
