package cn.iocoder.yudao.module.oa.service.phone;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.phone.PhoneCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.phone.PhoneRespVO;
import cn.iocoder.yudao.module.oa.api.dto.phone.PhoneUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.phone.PhoneDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.realname.RealnameDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.phone.PhoneMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.realname.RealnameMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhoneServiceImpl implements PhoneService {

    private static final String FILE_VIEW_PREFIX = "/admin-api/oa/file/view?key=";

    private final PhoneMapper phoneMapper;
    private final RealnameMapper realnameMapper;
    private final SysUserMapper sysUserMapper;
    private final AesUtil aesUtil;

    @Override
    public PageResult<PhoneRespVO> list(String phoneNumber, Long realnameId, String status, String phoneType,
                                        Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<PhoneDO> wrapper = new LambdaQueryWrapper<PhoneDO>()
                .eq(PhoneDO::getTenantId, tenantId)
                .eq(realnameId != null, PhoneDO::getRealnameId, realnameId)
                .eq(StrUtil.isNotBlank(status), PhoneDO::getStatus, status)
                .eq(StrUtil.isNotBlank(phoneType), PhoneDO::getPhoneType, phoneType)
                .orderByDesc(PhoneDO::getId);
        if (StrUtil.isNotBlank(phoneNumber)) {
            if (phoneNumber.matches("^1[3-9]\\d{9}$")) {
                wrapper.eq(PhoneDO::getPhoneNumberHash, hashPhone(phoneNumber));
            } else {
                wrapper.and(w -> w.like(PhoneDO::getPhoneCode, phoneNumber)
                        .or()
                        .like(PhoneDO::getPhoneModel, phoneNumber)
                        .or()
                        .like(PhoneDO::getDeviceNumber, phoneNumber)
                        .or()
                        .like(PhoneDO::getPurchaseBatch, phoneNumber));
            }
        }
        Page<PhoneDO> page = phoneMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        Map<Long, String> realNames = loadRealNames(page.getRecords());
        Map<Long, String> keeperNames = loadKeeperNames(page.getRecords());
        List<PhoneRespVO> list = page.getRecords().stream()
                .map(entity -> toResp(entity,
                        entity.getRealnameId() != null ? realNames.get(entity.getRealnameId()) : null,
                        entity.getKeeperId() != null ? keeperNames.get(entity.getKeeperId()) : null))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-phone", action = "create")
    public Long create(PhoneCreateReq req) {
        Long tenantId = requireTenantId();
        assertRealnameInTenant(req.getRealnameId(), tenantId);
        assertKeeperInTenant(req.getKeeperId(), tenantId);
        assertPhoneUnique(tenantId, req.getPhoneNumber(), null);

        PhoneDO entity = new PhoneDO();
        entity.setTenantId(tenantId);
        entity.setRealnameId(req.getRealnameId());
        entity.setPhoneNumberEncrypted(aesUtil.encrypt(req.getPhoneNumber()));
        entity.setPhoneNumberHash(hashPhone(req.getPhoneNumber()));
        entity.setPhoneCode(req.getPhoneCode());
        entity.setPhoneModel(req.getPhoneModel());
        entity.setSettingsScreenshotKey(sanitizeImageKey(req.getSettingsScreenshotKey(), tenantId));
        entity.setFrontImageKey(sanitizeImageKey(req.getFrontImageKey(), tenantId));
        entity.setBackImageKey(sanitizeImageKey(req.getBackImageKey(), tenantId));
        entity.setPurchaseBatch(req.getPurchaseBatch());
        entity.setPurchaseDate(req.getPurchaseDate());
        entity.setPurchaseTime(req.getPurchaseTime());
        entity.setHandlerName(req.getHandlerName());
        entity.setDeviceNumber(req.getDeviceNumber());
        entity.setIsAochuang(req.getIsAochuang());
        entity.setPhoneType(req.getPhoneType());
        entity.setKeeperId(req.getKeeperId());
        entity.setWechatBound(req.getWechatBound());
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setAccountBoundCount(0);
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        phoneMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-phone", action = "update")
    public void update(PhoneUpdateReq req) {
        PhoneDO existing = getRequiredInTenant(req.getId());
        if (req.getRealnameId() != null) {
            assertRealnameInTenant(req.getRealnameId(), existing.getTenantId());
            existing.setRealnameId(req.getRealnameId());
        }
        if (StrUtil.isNotBlank(req.getPhoneNumber())) {
            assertPhoneUnique(existing.getTenantId(), req.getPhoneNumber(), existing.getId());
            existing.setPhoneNumberEncrypted(aesUtil.encrypt(req.getPhoneNumber()));
            existing.setPhoneNumberHash(hashPhone(req.getPhoneNumber()));
        }
        if (req.getPhoneCode() != null) {
            existing.setPhoneCode(req.getPhoneCode());
        }
        if (req.getPhoneModel() != null) {
            existing.setPhoneModel(req.getPhoneModel());
        }
        if (req.getSettingsScreenshotKey() != null) {
            existing.setSettingsScreenshotKey(sanitizeImageKey(req.getSettingsScreenshotKey(), existing.getTenantId()));
        }
        if (req.getFrontImageKey() != null) {
            existing.setFrontImageKey(sanitizeImageKey(req.getFrontImageKey(), existing.getTenantId()));
        }
        if (req.getBackImageKey() != null) {
            existing.setBackImageKey(sanitizeImageKey(req.getBackImageKey(), existing.getTenantId()));
        }
        if (req.getPurchaseBatch() != null) {
            existing.setPurchaseBatch(req.getPurchaseBatch());
        }
        if (req.getPurchaseDate() != null) {
            existing.setPurchaseDate(req.getPurchaseDate());
        }
        if (req.getPurchaseTime() != null) {
            existing.setPurchaseTime(req.getPurchaseTime());
        }
        if (req.getHandlerName() != null) {
            existing.setHandlerName(req.getHandlerName());
        }
        if (req.getDeviceNumber() != null) {
            existing.setDeviceNumber(req.getDeviceNumber());
        }
        if (req.getIsAochuang() != null) {
            existing.setIsAochuang(req.getIsAochuang());
        }
        if (req.getPhoneType() != null) {
            existing.setPhoneType(req.getPhoneType());
        }
        if (req.getKeeperId() != null) {
            assertKeeperInTenant(req.getKeeperId(), existing.getTenantId());
            existing.setKeeperId(req.getKeeperId());
        }
        if (req.getWechatBound() != null) {
            existing.setWechatBound(req.getWechatBound());
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        phoneMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-phone", action = "delete")
    public void delete(Long id) {
        PhoneDO existing = getRequiredInTenant(id);
        if (existing.getAccountBoundCount() != null && existing.getAccountBoundCount() > 0) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND);
        }
        phoneMapper.deleteById(id);
    }

    private PhoneDO getRequiredInTenant(Long id) {
        PhoneDO entity = phoneMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        Long tenantId = requireTenantId();
        if (!tenantId.equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private void assertPhoneUnique(Long tenantId, String phoneNumber, Long excludeId) {
        LambdaQueryWrapper<PhoneDO> wrapper = new LambdaQueryWrapper<PhoneDO>()
                .eq(PhoneDO::getTenantId, tenantId)
                .eq(PhoneDO::getPhoneNumberHash, hashPhone(phoneNumber));
        if (excludeId != null) {
            wrapper.ne(PhoneDO::getId, excludeId);
        }
        if (phoneMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY);
        }
    }

    private void assertRealnameInTenant(Long realnameId, Long tenantId) {
        if (realnameId == null) {
            return;
        }
        RealnameDO realname = realnameMapper.selectById(realnameId);
        if (realname == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "关联实名人不存在");
        }
        if (!tenantId.equals(realname.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
    }

    private void assertKeeperInTenant(Long keeperId, Long tenantId) {
        SysUserDO user = sysUserMapper.selectById(keeperId);
        if (user == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "保管人不存在");
        }
        if (!tenantId.equals(user.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
    }

    private Map<Long, String> loadRealNames(List<PhoneDO> records) {
        List<Long> ids = records.stream()
                .map(PhoneDO::getRealnameId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<RealnameDO> list = realnameMapper.selectBatchIds(ids);
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }
        return list.stream().collect(Collectors.toMap(RealnameDO::getId, RealnameDO::getRealName, (a, b) -> a));
    }

    private Map<Long, String> loadKeeperNames(List<PhoneDO> records) {
        List<Long> ids = records.stream()
                .map(PhoneDO::getKeeperId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUserDO> list = sysUserMapper.selectBatchIds(ids);
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }
        return list.stream().collect(Collectors.toMap(SysUserDO::getId, SysUserDO::getNickname, (a, b) -> a));
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return tenantId;
    }

    private String hashPhone(String phoneNumber) {
        return DigestUtil.sha256Hex(phoneNumber);
    }

    private PhoneRespVO toResp(PhoneDO entity, String realName, String keeperName) {
        PhoneRespVO vo = new PhoneRespVO();
        vo.setId(entity.getId());
        vo.setRealnameId(entity.getRealnameId());
        vo.setRealName(realName);
        vo.setPhoneNumberMasked(maskPhone(entity.getPhoneNumberEncrypted()));
        vo.setPhoneCode(entity.getPhoneCode());
        vo.setPhoneModel(entity.getPhoneModel());
        vo.setSettingsScreenshotKey(entity.getSettingsScreenshotKey());
        vo.setSettingsScreenshotUrl(toFileViewUrl(entity.getSettingsScreenshotKey()));
        vo.setFrontImageKey(entity.getFrontImageKey());
        vo.setFrontImageUrl(toFileViewUrl(entity.getFrontImageKey()));
        vo.setBackImageKey(entity.getBackImageKey());
        vo.setBackImageUrl(toFileViewUrl(entity.getBackImageKey()));
        vo.setPurchaseBatch(entity.getPurchaseBatch());
        vo.setPurchaseDate(entity.getPurchaseDate());
        vo.setPurchaseTime(entity.getPurchaseTime());
        vo.setHandlerName(entity.getHandlerName());
        vo.setDeviceNumber(entity.getDeviceNumber());
        vo.setIsAochuang(entity.getIsAochuang());
        vo.setPhoneType(entity.getPhoneType());
        vo.setKeeperId(entity.getKeeperId());
        vo.setKeeperName(keeperName);
        vo.setWechatBound(entity.getWechatBound());
        vo.setStatus(entity.getStatus());
        vo.setAccountBoundCount(entity.getAccountBoundCount());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private String maskPhone(String encrypted) {
        if (StrUtil.isBlank(encrypted)) {
            return null;
        }
        try {
            String plain = aesUtil.decrypt(encrypted);
            if (plain.length() != 11) {
                return "****";
            }
            return plain.substring(0, 3) + "****" + plain.substring(7);
        } catch (Exception ex) {
            return "****";
        }
    }

    private String sanitizeImageKey(String key, Long tenantId) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        if (key.contains("..") || !key.startsWith(tenantId + "/")) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN.getCode(), "图片文件无效");
        }
        return key;
    }

    private String toFileViewUrl(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        return FILE_VIEW_PREFIX + key;
    }
}
