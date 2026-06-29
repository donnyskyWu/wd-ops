package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.account.DouyinFollowerRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.DouyinFollowerDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.DouyinFollowerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DouyinFollowerQueryServiceImpl implements DouyinFollowerQueryService {

    private static final String PLATFORM_DOUYIN = "DOUYIN";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DouyinFollowerMapper douyinFollowerMapper;
    private final AccountDataScopeChecker accountDataScopeChecker;

    @Override
    public PageResult<DouyinFollowerRespVO> pageByAccount(Long accountId, Integer pageNo, Integer pageSize) {
        AccountDO account = accountDataScopeChecker.requireReadableAccount(accountId);
        assertDouyinPlatform(account);
        Long tenantId = account.getTenantId();

        Page<DouyinFollowerDO> page = douyinFollowerMapper.selectPage(
                new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<DouyinFollowerDO>()
                        .eq(DouyinFollowerDO::getTenantId, tenantId)
                        .eq(DouyinFollowerDO::getAccountId, accountId)
                        .orderByDesc(DouyinFollowerDO::getFollowedAt)
                        .orderByDesc(DouyinFollowerDO::getId));

        List<DouyinFollowerRespVO> list = page.getRecords().stream()
                .map(this::toResp)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private DouyinFollowerRespVO toResp(DouyinFollowerDO entity) {
        DouyinFollowerRespVO vo = new DouyinFollowerRespVO();
        vo.setId(entity.getId());
        vo.setFollowerId(entity.getFollowerId());
        vo.setNickname(entity.getNickname());
        vo.setAvatar(entity.getAvatar());
        vo.setFollowedAt(formatDateTime(entity.getFollowedAt()));
        vo.setSyncedAt(formatDateTime(entity.getSyncedAt()));
        return vo;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DT_FMT);
    }

    private void assertDouyinPlatform(AccountDO account) {
        if (!PLATFORM_DOUYIN.equals(account.getPlatformType())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "仅抖音账号支持粉丝列表查询");
        }
    }
}
