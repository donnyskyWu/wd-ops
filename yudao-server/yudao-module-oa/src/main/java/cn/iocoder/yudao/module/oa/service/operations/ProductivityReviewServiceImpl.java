package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.operations.ProductivityReviewVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductivityReviewServiceImpl implements ProductivityReviewService {

    private final SysUserMapper sysUserMapper;
    private final IpGroupMapper ipGroupMapper;

    @Override
    public PageResult<ProductivityReviewVO> list(Long ipGroupId, Long userId, Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getTenantId, tenantId)
                .eq(ipGroupId != null, SysUserDO::getIpGroupId, ipGroupId)
                .eq(userId != null, SysUserDO::getId, userId)
                .orderByAsc(SysUserDO::getId);
        Page<SysUserDO> page = sysUserMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()), page.getTotal());
    }

    private ProductivityReviewVO toVO(SysUserDO user) {
        ProductivityReviewVO vo = new ProductivityReviewVO();
        vo.setUserId(user.getId());
        vo.setUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
        vo.setPosition(user.getPosition());
        vo.setIpGroupId(user.getIpGroupId());
        if (user.getIpGroupId() != null) {
            IpGroupDO group = ipGroupMapper.selectById(user.getIpGroupId());
            if (group != null) {
                vo.setIpGroupName(group.getGroupName());
            }
        }
        return vo;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
