package cn.iocoder.yudao.module.oa.service.operations;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.operations.AccountAnalysisVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.FollowerDailyMapper;
import cn.iocoder.yudao.module.oa.framework.auth.DataScopeSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountAnalysisServiceImpl implements AccountAnalysisService {

    private final AccountMapper accountMapper;
    private final IpGroupMapper ipGroupMapper;
    private final FollowerDailyMapper followerDailyMapper;
    private final ContentMapper contentMapper;

    @Override
    public PageResult<AccountAnalysisVO> list(String platform, Long ipGroupId, String keyword,
                                              Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .eq(StrUtil.isNotBlank(platform) && !"ALL".equalsIgnoreCase(platform),
                        AccountDO::getPlatformType, platform)
                .eq(ipGroupId != null, AccountDO::getIpGroupId, ipGroupId)
                .like(StrUtil.isNotBlank(keyword), AccountDO::getAccountName, keyword)
                .orderByDesc(AccountDO::getId);
        DataScopeSupport.applyIpGroupScope(wrapper, AccountDO::getIpGroupId);

        Page<AccountDO> page = accountMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);
        List<AccountAnalysisVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private AccountAnalysisVO toVO(AccountDO account) {
        AccountAnalysisVO vo = new AccountAnalysisVO();
        vo.setAccountId(account.getId());
        vo.setAccountName(account.getAccountName());
        vo.setPlatformType(account.getPlatformType());
        vo.setIpGroupId(account.getIpGroupId());
        if (account.getIpGroupId() != null) {
            IpGroupDO group = ipGroupMapper.selectById(account.getIpGroupId());
            if (group != null) {
                vo.setIpGroupName(group.getGroupName());
            }
        }
        vo.setAccountType(account.getAccountType());
        vo.setStatus(account.getStatus());

        FollowerDailyDO latest = followerDailyMapper.selectOne(new LambdaQueryWrapper<FollowerDailyDO>()
                .eq(FollowerDailyDO::getTenantId, account.getTenantId())
                .eq(FollowerDailyDO::getAccountId, account.getId())
                .orderByDesc(FollowerDailyDO::getStatDate)
                .last("LIMIT 1"));
        vo.setFollowerCount(latest != null ? latest.getFollowerCount() : 0L);

        long contentCount = contentMapper.selectCount(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, account.getTenantId())
                .eq(ContentDO::getAccountId, account.getId()));
        vo.setContentCount(Math.toIntExact(contentCount));
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
