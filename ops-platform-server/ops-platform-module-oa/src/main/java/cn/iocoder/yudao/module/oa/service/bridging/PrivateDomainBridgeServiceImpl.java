package cn.iocoder.yudao.module.oa.service.bridging;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.bridging.PrivateDomainBridgeCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.bridging.PrivateDomainBridgeRejectReq;
import cn.iocoder.yudao.module.oa.api.dto.bridging.PrivateDomainBridgeRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.bridging.PrivateDomainConversionBridgeDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.AochuangFriendDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.phone.PhoneDO;
import cn.iocoder.yudao.module.oa.dal.mysql.bridging.PrivateDomainConversionBridgeMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.AochuangFriendMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.phone.PhoneMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrivateDomainBridgeServiceImpl implements PrivateDomainBridgeService {

    private static final String REVIEW_PENDING = "PENDING";
    private static final String REVIEW_APPROVED = "APPROVED";
    private static final String REVIEW_REJECTED = "REJECTED";
    private static final String MATCH_MANUAL = "MANUAL";
    private static final String IDENTITY_AOCHUANG_FRIEND = "AOCHUANG_FRIEND";
    private static final String IDENTITY_PHONE = "PHONE";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PrivateDomainConversionBridgeMapper bridgeMapper;
    private final AochuangFriendMapper aochuangFriendMapper;
    private final PhoneMapper phoneMapper;

    @Override
    public PageResult<PrivateDomainBridgeRespVO> page(String reviewStatus, String sourceType, String targetType,
                                                      String matchMethod, Integer pageNo, Integer pageSize) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<PrivateDomainConversionBridgeDO> wrapper = new LambdaQueryWrapper<PrivateDomainConversionBridgeDO>()
                .eq(PrivateDomainConversionBridgeDO::getTenantId, tenantId)
                .eq(StrUtil.isNotBlank(reviewStatus), PrivateDomainConversionBridgeDO::getReviewStatus, reviewStatus)
                .eq(StrUtil.isNotBlank(sourceType), PrivateDomainConversionBridgeDO::getSourceType, sourceType)
                .eq(StrUtil.isNotBlank(targetType), PrivateDomainConversionBridgeDO::getTargetType, targetType)
                .eq(StrUtil.isNotBlank(matchMethod), PrivateDomainConversionBridgeDO::getMatchMethod, matchMethod)
                .orderByDesc(PrivateDomainConversionBridgeDO::getId);
        Page<PrivateDomainConversionBridgeDO> page = bridgeMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);
        List<PrivateDomainBridgeRespVO> list = page.getRecords().stream()
                .map(this::toResp)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public PrivateDomainBridgeRespVO get(Long id) {
        return toResp(getRequiredInTenant(id));
    }

    @Override
    @Transactional
    @AuditLog(module = "M10-private-domain-bridge", action = "create")
    public Long create(PrivateDomainBridgeCreateReq req) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        assertEntityInTenant(req.getSourceType(), req.getSourceId(), tenantId);
        assertEntityInTenant(req.getTargetType(), req.getTargetId(), tenantId);
        if (existsPair(tenantId, req.getSourceType(), req.getSourceId(), req.getTargetType(), req.getTargetId())) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY);
        }
        return createBridge(tenantId, req.getSourceType(), req.getSourceId(), req.getTargetType(), req.getTargetId(),
                StrUtil.blankToDefault(req.getMatchMethod(), MATCH_MANUAL),
                req.getConfidence(), req.getMatchEvidenceJson(), REVIEW_PENDING);
    }

    @Override
    @Transactional
    @AuditLog(module = "M10-private-domain-bridge", action = "confirm")
    public void confirm(Long id) {
        PrivateDomainConversionBridgeDO entity = getRequiredInTenant(id);
        if (!REVIEW_PENDING.equals(entity.getReviewStatus())) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "仅待审核记录可确认");
        }
        entity.setReviewStatus(REVIEW_APPROVED);
        entity.setLinkedBy(ConfigTenantSupport.currentUsername());
        entity.setLinkedAt(LocalDateTime.now());
        ConfigTenantSupport.fillUpdate(entity);
        bridgeMapper.updateById(entity);
    }

    @Override
    @Transactional
    @AuditLog(module = "M10-private-domain-bridge", action = "reject")
    public void reject(Long id, PrivateDomainBridgeRejectReq req) {
        PrivateDomainConversionBridgeDO entity = getRequiredInTenant(id);
        if (!REVIEW_PENDING.equals(entity.getReviewStatus())) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "仅待审核记录可驳回");
        }
        entity.setReviewStatus(REVIEW_REJECTED);
        entity.setLinkedBy(ConfigTenantSupport.currentUsername());
        entity.setLinkedAt(LocalDateTime.now());
        if (req != null && StrUtil.isNotBlank(req.getReason())) {
            entity.setMatchEvidenceJson(mergeRejectReason(entity.getMatchEvidenceJson(), req.getReason()));
        }
        ConfigTenantSupport.fillUpdate(entity);
        bridgeMapper.updateById(entity);
    }

    @Override
    public boolean existsPair(Long tenantId, String sourceType, Long sourceId, String targetType, Long targetId) {
        return bridgeMapper.selectCount(new LambdaQueryWrapper<PrivateDomainConversionBridgeDO>()
                .eq(PrivateDomainConversionBridgeDO::getTenantId, tenantId)
                .eq(PrivateDomainConversionBridgeDO::getSourceType, sourceType)
                .eq(PrivateDomainConversionBridgeDO::getSourceId, sourceId)
                .eq(PrivateDomainConversionBridgeDO::getTargetType, targetType)
                .eq(PrivateDomainConversionBridgeDO::getTargetId, targetId)) > 0;
    }

    @Override
    @Transactional
    public Long createBridge(Long tenantId, String sourceType, Long sourceId, String targetType, Long targetId,
                             String matchMethod, BigDecimal confidence, String evidenceJson, String reviewStatus) {
        PrivateDomainConversionBridgeDO entity = new PrivateDomainConversionBridgeDO();
        entity.setTenantId(tenantId);
        entity.setSourceType(sourceType);
        entity.setSourceId(sourceId);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setMatchMethod(matchMethod);
        entity.setConfidence(confidence);
        entity.setMatchEvidenceJson(evidenceJson);
        entity.setReviewStatus(reviewStatus);
        if (REVIEW_APPROVED.equals(reviewStatus)) {
            entity.setLinkedBy(ConfigTenantSupport.currentUsername());
            entity.setLinkedAt(LocalDateTime.now());
        }
        ConfigTenantSupport.fillCreate(entity);
        bridgeMapper.insert(entity);
        return entity.getId();
    }

    private PrivateDomainConversionBridgeDO getRequiredInTenant(Long id) {
        PrivateDomainConversionBridgeDO entity = bridgeMapper.selectById(id);
        return ConfigTenantSupport.getRequiredInTenant(entity);
    }

    private void assertEntityInTenant(String identityType, Long entityId, Long tenantId) {
        if (IDENTITY_AOCHUANG_FRIEND.equals(identityType)) {
            AochuangFriendDO friend = aochuangFriendMapper.selectById(entityId);
            if (friend == null || !tenantId.equals(friend.getTenantId())) {
                throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
            }
            return;
        }
        if (IDENTITY_PHONE.equals(identityType)) {
            PhoneDO phone = phoneMapper.selectById(entityId);
            if (phone == null || !tenantId.equals(phone.getTenantId())) {
                throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
            }
            return;
        }
        throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                "身份类型 " + identityType + " 实体校验待后续通道实现（P2）");
    }

    private PrivateDomainBridgeRespVO toResp(PrivateDomainConversionBridgeDO entity) {
        PrivateDomainBridgeRespVO vo = new PrivateDomainBridgeRespVO();
        vo.setId(entity.getId());
        vo.setSourceType(entity.getSourceType());
        vo.setSourceId(entity.getSourceId());
        vo.setSourceLabel(resolveLabel(entity.getSourceType(), entity.getSourceId()));
        vo.setTargetType(entity.getTargetType());
        vo.setTargetId(entity.getTargetId());
        vo.setTargetLabel(resolveLabel(entity.getTargetType(), entity.getTargetId()));
        vo.setMatchMethod(entity.getMatchMethod());
        vo.setConfidence(entity.getConfidence());
        vo.setMatchEvidenceJson(entity.getMatchEvidenceJson());
        vo.setReviewStatus(entity.getReviewStatus());
        vo.setLinkedBy(entity.getLinkedBy());
        if (entity.getLinkedAt() != null) {
            vo.setLinkedAt(entity.getLinkedAt().format(DT_FMT));
        }
        if (entity.getCreateTime() != null) {
            vo.setCreateTime(entity.getCreateTime().format(DT_FMT));
        }
        return vo;
    }

    private String resolveLabel(String identityType, Long entityId) {
        if (entityId == null) {
            return "";
        }
        if (IDENTITY_AOCHUANG_FRIEND.equals(identityType)) {
            AochuangFriendDO friend = aochuangFriendMapper.selectById(entityId);
            if (friend != null) {
                return StrUtil.blankToDefault(friend.getNickname(), friend.getAochuangFriendId());
            }
        }
        if (IDENTITY_PHONE.equals(identityType)) {
            PhoneDO phone = phoneMapper.selectById(entityId);
            if (phone != null) {
                return StrUtil.blankToDefault(phone.getPhoneCode(), "手机#" + entityId);
            }
        }
        return identityType + "#" + entityId;
    }

    private static String mergeRejectReason(String evidenceJson, String reason) {
        if (StrUtil.isBlank(evidenceJson)) {
            return "{\"rejectReason\":\"" + reason.replace("\"", "\\\"") + "\"}";
        }
        if (evidenceJson.endsWith("}")) {
            return evidenceJson.substring(0, evidenceJson.length() - 1)
                    + ",\"rejectReason\":\"" + reason.replace("\"", "\\\"") + "\"}";
        }
        return evidenceJson;
    }
}
