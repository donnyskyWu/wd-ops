package cn.iocoder.yudao.module.oa.service.ipgroup;

import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupAccountBindReq;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupAccountVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupAnchorBindReq;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupAnchorVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupListVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupMemberCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupMemberUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupMemberVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupStatsVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupTreeVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupUpdateReq;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

import java.util.List;

public interface IpGroupService {

    List<IpGroupTreeVO> getTree();

    /**
     * S-R12 修复：分页查询 IP 组列表（spec API-M1 §2.2 定义，但 controller 漏实现）
     */
    PageResult<IpGroupListVO> listPage(String groupName, Integer groupType, Integer status,
                                       Integer pageNum, Integer pageSize);

    IpGroupDetailVO getDetail(Long id);

    IpGroupStatsVO getStats(Long id);

    List<IpGroupAccountVO> listAccounts(Long id);

    Long create(IpGroupCreateReq req);

    void update(IpGroupUpdateReq req);

    void updateStatus(Long id, Integer status);

    void delete(Long id);

    List<IpGroupMemberVO> listMembers(Long id);

    void addMember(Long groupId, IpGroupMemberCreateReq req);

    void updateMember(Long groupId, Long memberId, IpGroupMemberUpdateReq req);

    void deleteMember(Long groupId, Long memberId);

    void bindAccounts(Long groupId, IpGroupAccountBindReq req);

    void unbindAccount(Long groupId, Long accountId);

    List<IpGroupAnchorVO> listAnchors(Long id);

    void bindAnchors(Long groupId, IpGroupAnchorBindReq req);

    void unbindAnchor(Long groupId, Long anchorUserId);
}
