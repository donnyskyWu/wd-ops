package cn.iocoder.yudao.module.oa.service.operations;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.operations.AccountAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerAnalysisVO;
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

import java.time.LocalDate;
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

    @Override
    public List<FollowerAnalysisVO> listAccountFollowers(Long accountId, LocalDate startDate, LocalDate endDate) {
        Long tenantId = requireTenantId();
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null || !account.getTenantId().equals(tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "账号不存在");
        }
        String ipGroupName = null;
        if (account.getIpGroupId() != null) {
            IpGroupDO g = ipGroupMapper.selectById(account.getIpGroupId());
            if (g != null) ipGroupName = g.getGroupName();
        }
        final String finalIpGroupName = ipGroupName;
        LambdaQueryWrapper<FollowerDailyDO> wrapper = new LambdaQueryWrapper<FollowerDailyDO>()
                .eq(FollowerDailyDO::getTenantId, tenantId)
                .eq(FollowerDailyDO::getAccountId, accountId)
                .ge(startDate != null, FollowerDailyDO::getStatDate, startDate)
                .le(endDate != null, FollowerDailyDO::getStatDate, endDate)
                .orderByDesc(FollowerDailyDO::getStatDate);
        DataScopeSupport.applyIpGroupScope(wrapper, FollowerDailyDO::getAccountId);
        return followerDailyMapper.selectList(wrapper).stream().map(d -> {
            FollowerAnalysisVO vo = new FollowerAnalysisVO();
            vo.setStatDate(d.getStatDate());
            vo.setAccountId(d.getAccountId());
            vo.setAccountName(account.getAccountName());
            vo.setIpGroupName(finalIpGroupName);
            vo.setFollowerCount(d.getFollowerCount());
            vo.setNewFollower(d.getNewFollower());
            vo.setUnfollowCount(d.getUnfollowCount());
            vo.setNetGrowth(d.getNetGrowth());
            vo.setGrowthRate(d.getGrowthRate());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PageResult<ContentAnalysisVO> listAccountContents(Long accountId, Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null || !account.getTenantId().equals(tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "账号不存在");
        }
        LambdaQueryWrapper<ContentDO> wrapper = new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .eq(ContentDO::getAccountId, accountId)
                .orderByDesc(ContentDO::getPublishTime);
        DataScopeSupport.applyIpGroupScope(wrapper, ContentDO::getAccountId);
        Page<ContentDO> page = contentMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);
        List<ContentAnalysisVO> list = page.getRecords().stream().map(this::toContentVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private ContentAnalysisVO toContentVO(ContentDO c) {
        ContentAnalysisVO vo = new ContentAnalysisVO();
        vo.setId(c.getId());
        vo.setAccountId(c.getAccountId());
        vo.setAccountName(null); // 列表已用 accountName 上下文
        vo.setTitle(c.getTitle());
        vo.setPlatformType(c.getPlatformType());
        vo.setContentType(c.getContentType());
        vo.setPublishTime(c.getPublishTime());
        vo.setReadCount(c.getReadCount());
        vo.setLikeCount(c.getLikeCount());
        vo.setCommentCount(c.getCommentCount());
        vo.setForwardCount(c.getForwardCount());
        vo.setIsHit(c.getIsHit() != null && c.getIsHit() == 1);
        vo.setDataSource(c.getDataSource());
        return vo;
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

