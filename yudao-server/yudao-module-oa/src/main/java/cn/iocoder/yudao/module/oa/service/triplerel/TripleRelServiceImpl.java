package cn.iocoder.yudao.module.oa.service.triplerel;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.triplerel.TripleRelCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.triplerel.TripleRelGraphRespVO;
import cn.iocoder.yudao.module.oa.api.dto.triplerel.TripleRelRespVO;
import cn.iocoder.yudao.module.oa.api.dto.triplerel.TripleRelStatisticsVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.WeworkAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.triplerel.TripleRelDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.PersonalWechatAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.WeworkAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.triplerel.TripleRelMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripleRelServiceImpl implements TripleRelService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TripleRelMapper tripleRelMapper;
    private final PersonalWechatAccountMapper personalWechatAccountMapper;
    private final AccountMapper accountMapper;
    private final WeworkAccountMapper weworkAccountMapper;

    @Override
    public PageResult<TripleRelRespVO> list(String relationType, Integer status, Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<TripleRelDO> wrapper = new LambdaQueryWrapper<TripleRelDO>()
                .eq(TripleRelDO::getTenantId, tenantId)
                .eq(StrUtil.isNotBlank(relationType), TripleRelDO::getRelationType, relationType)
                .eq(status != null, TripleRelDO::getStatus, status)
                .orderByDesc(TripleRelDO::getId);
        Page<TripleRelDO> page = tripleRelMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<TripleRelRespVO> list = page.getRecords().stream().map(this::toResp).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public TripleRelGraphRespVO graph(Long personalWechatId) {
        PersonalWechatAccountDO wechat = getPersonalWechatInTenant(personalWechatId);
        Long tenantId = requireTenantId();

        List<TripleRelDO> rels = tripleRelMapper.selectList(new LambdaQueryWrapper<TripleRelDO>()
                .eq(TripleRelDO::getTenantId, tenantId)
                .eq(TripleRelDO::getWechatAccountId, personalWechatId)
                .eq(TripleRelDO::getStatus, 1));

        TripleRelGraphRespVO vo = new TripleRelGraphRespVO();
        vo.setPersonalWechatId(personalWechatId);
        TripleRelGraphRespVO.PersonalWechatNode pw = new TripleRelGraphRespVO.PersonalWechatNode();
        pw.setId(wechat.getId());
        pw.setWechatId(wechat.getWechatId());
        pw.setAccountName(wechat.getAccountName());
        vo.setPersonalWechat(pw);

        List<TripleRelGraphRespVO.VideoAccountNode> videos = new ArrayList<>();
        TripleRelGraphRespVO.WeworkAccountNode weworkNode = null;
        for (TripleRelDO rel : rels) {
            if (rel.getVideoAccountId() != null) {
                AccountDO video = accountMapper.selectById(rel.getVideoAccountId());
                if (video != null) {
                    TripleRelGraphRespVO.VideoAccountNode node = new TripleRelGraphRespVO.VideoAccountNode();
                    node.setId(video.getId());
                    node.setAccountName(video.getAccountName());
                    videos.add(node);
                }
            }
            if (rel.getWeworkAccountId() != null && weworkNode == null) {
                WeworkAccountDO wework = weworkAccountMapper.selectById(rel.getWeworkAccountId());
                if (wework != null) {
                    weworkNode = new TripleRelGraphRespVO.WeworkAccountNode();
                    weworkNode.setId(wework.getId());
                    weworkNode.setAccountName(wework.getAccountName());
                    weworkNode.setCorpId(wework.getCorpId());
                }
            }
        }
        vo.setVideoAccounts(videos);
        vo.setWeworkAccount(weworkNode);
        return vo;
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-triple-rel", action = "create")
    public Long create(TripleRelCreateReq req) {
        Long tenantId = requireTenantId();
        validateCreateReq(req, tenantId);

        List<Long> videoIds = CollUtil.isEmpty(req.getVideoAccountIds())
                ? Collections.singletonList(null) : req.getVideoAccountIds();

        Long firstId = null;
        for (Long videoId : videoIds) {
            TripleRelDO entity = new TripleRelDO();
            entity.setTenantId(tenantId);
            entity.setWechatAccountId(req.getPersonalWechatId());
            entity.setVideoAccountId(videoId);
            entity.setWeworkAccountId(req.getWeworkAccountId());
            entity.setRelationType(req.getRelationType());
            entity.setBindTime(LocalDateTime.now());
            entity.setStatus(1);
            entity.setCreator(TenantContextHolder.getUsername());
            entity.setUpdater(TenantContextHolder.getUsername());
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            tripleRelMapper.insert(entity);
            if (firstId == null) {
                firstId = entity.getId();
            }
        }
        return firstId;
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-triple-rel", action = "unbind")
    public void unbind(Long id) {
        TripleRelDO entity = getRequiredInTenant(id);
        entity.setStatus(0);
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setUpdateTime(LocalDateTime.now());
        tripleRelMapper.updateById(entity);
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-triple-rel", action = "rebind")
    public void rebind(Long id) {
        TripleRelDO entity = getRequiredInTenant(id);
        entity.setStatus(1);
        entity.setBindTime(LocalDateTime.now());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setUpdateTime(LocalDateTime.now());
        tripleRelMapper.updateById(entity);
    }

    @Override
    public TripleRelStatisticsVO statistics() {
        Long tenantId = requireTenantId();
        List<TripleRelDO> all = tripleRelMapper.selectList(new LambdaQueryWrapper<TripleRelDO>()
                .eq(TripleRelDO::getTenantId, tenantId));

        TripleRelStatisticsVO vo = new TripleRelStatisticsVO();
        vo.setTotalBound(all.stream().filter(r -> r.getStatus() != null && r.getStatus() == 1).count());
        vo.setUnbound(all.stream().filter(r -> r.getStatus() != null && r.getStatus() == 0).count());
        vo.setFullTriple(countByType(all, "FULL_TRIPLE", 1));
        vo.setWechatVideo(countByType(all, "WECHAT_VIDEO", 1));
        vo.setWechatWework(countByType(all, "WECHAT_WEWORK", 1));
        vo.setVideoWework(countByType(all, "VIDEO_WEWORK", 1));
        return vo;
    }

    private long countByType(List<TripleRelDO> all, String type, int status) {
        return all.stream()
                .filter(r -> type.equals(r.getRelationType()) && r.getStatus() != null && r.getStatus() == status)
                .count();
    }

    private void validateCreateReq(TripleRelCreateReq req, Long tenantId) {
        String type = req.getRelationType();
        if (StrUtil.isBlank(type)) {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID);
        }
        switch (type) {
            case "FULL_TRIPLE" -> {
                if (req.getPersonalWechatId() == null || req.getWeworkAccountId() == null
                        || CollUtil.isEmpty(req.getVideoAccountIds())) {
                    throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "完整三方需个微、视频号、企微");
                }
            }
            case "WECHAT_VIDEO" -> {
                if (req.getPersonalWechatId() == null || CollUtil.isEmpty(req.getVideoAccountIds())) {
                    throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "微信+视频需个微与视频号");
                }
            }
            case "WECHAT_WEWORK" -> {
                if (req.getPersonalWechatId() == null || req.getWeworkAccountId() == null) {
                    throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "微信+企微需个微与企微");
                }
            }
            case "VIDEO_WEWORK" -> {
                if (req.getWeworkAccountId() == null || CollUtil.isEmpty(req.getVideoAccountIds())) {
                    throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "视频+企微需视频号与企微");
                }
            }
            default -> throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID);
        }
        if (req.getPersonalWechatId() != null) {
            getPersonalWechatInTenant(req.getPersonalWechatId());
        }
        if (req.getWeworkAccountId() != null) {
            getWeworkInTenant(req.getWeworkAccountId());
        }
        if (CollUtil.isNotEmpty(req.getVideoAccountIds())) {
            for (Long videoId : req.getVideoAccountIds()) {
                getVideoAccountInTenant(videoId, tenantId);
            }
        }
    }

    private TripleRelRespVO toResp(TripleRelDO entity) {
        TripleRelRespVO vo = new TripleRelRespVO();
        vo.setId(entity.getId());
        vo.setWechatAccountId(entity.getWechatAccountId());
        vo.setVideoAccountId(entity.getVideoAccountId());
        vo.setWeworkAccountId(entity.getWeworkAccountId());
        vo.setRelationType(entity.getRelationType());
        vo.setStatus(entity.getStatus());
        if (entity.getBindTime() != null) {
            vo.setBindTime(entity.getBindTime().format(DT_FMT));
        }
        if (entity.getWechatAccountId() != null) {
            PersonalWechatAccountDO w = personalWechatAccountMapper.selectById(entity.getWechatAccountId());
            if (w != null) {
                vo.setWechatName(w.getAccountName() + "(" + w.getWechatId() + ")");
            }
        }
        if (entity.getVideoAccountId() != null) {
            AccountDO v = accountMapper.selectById(entity.getVideoAccountId());
            if (v != null) {
                vo.setVideoName(v.getAccountName());
            }
        }
        if (entity.getWeworkAccountId() != null) {
            WeworkAccountDO ww = weworkAccountMapper.selectById(entity.getWeworkAccountId());
            if (ww != null) {
                vo.setWeworkName(ww.getAccountName());
            }
        }
        return vo;
    }

    private PersonalWechatAccountDO getPersonalWechatInTenant(Long id) {
        PersonalWechatAccountDO entity = personalWechatAccountMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!requireTenantId().equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private WeworkAccountDO getWeworkInTenant(Long id) {
        WeworkAccountDO entity = weworkAccountMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!requireTenantId().equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private AccountDO getVideoAccountInTenant(Long id, Long tenantId) {
        AccountDO entity = accountMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!tenantId.equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        if (!"WECHAT_VIDEO".equals(entity.getPlatformType())) {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID.getCode(), "视频号平台类型不匹配");
        }
        return entity;
    }

    private TripleRelDO getRequiredInTenant(Long id) {
        TripleRelDO entity = tripleRelMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!requireTenantId().equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return tenantId;
    }
}
