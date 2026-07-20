package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.account.MpFollowerRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.MpUserDO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WechatMpFollowerQueryServiceImpl implements WechatMpFollowerQueryService {

    private static final String PLATFORM_WECHAT_OFFICIAL = "WECHAT_OFFICIAL";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MpUserDataService mpUserDataService;
    private final WechatOfficialAccountResolver wechatOfficialAccountResolver;
    private final AccountDataScopeChecker accountDataScopeChecker;

    @Override
    public PageResult<MpFollowerRespVO> pageByAccount(Long accountId, Integer pageNo, Integer pageSize) {
        AccountDO account = accountDataScopeChecker.requireReadableAccount(accountId);
        assertWechatOfficialPlatform(account);
        Long tenantId = account.getTenantId();
        Long mpAccountId = wechatOfficialAccountResolver.resolveMpAccountId(accountId, tenantId)
                .orElseThrow(() -> new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(),
                        "未找到关联 Football 公众号"));

        Page<MpUserDO> page = mpUserDataService.selectPageByAccount(
                new Page<>(pageNo, pageSize), tenantId, mpAccountId);

        List<MpFollowerRespVO> list = page.getRecords().stream()
                .map(this::toResp)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private MpFollowerRespVO toResp(MpUserDO entity) {
        MpFollowerRespVO vo = new MpFollowerRespVO();
        vo.setId(entity.getId());
        vo.setOpenid(entity.getOpenid());
        vo.setNickname(entity.getNickname());
        vo.setAvatar(entity.getHeadImageUrl());
        vo.setSubscribedAt(formatDateTime(entity.getSubscribeTime()));
        vo.setSyncedAt(formatDateTime(entity.getUpdateTime()));
        return vo;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DT_FMT);
    }

    private void assertWechatOfficialPlatform(AccountDO account) {
        if (!PLATFORM_WECHAT_OFFICIAL.equals(account.getPlatformType())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "仅公众号账号支持粉丝列表查询");
        }
    }
}
