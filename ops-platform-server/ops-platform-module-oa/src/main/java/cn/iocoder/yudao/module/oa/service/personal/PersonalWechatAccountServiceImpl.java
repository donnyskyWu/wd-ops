package cn.iocoder.yudao.module.oa.service.personal;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatApiConfigReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.phone.PhoneDO;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.PersonalWechatAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.phone.PhoneMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalWechatAccountServiceImpl implements PersonalWechatAccountService {

    private static final String MASK = "****";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PersonalWechatAccountMapper personalWechatAccountMapper;
    private final PhoneMapper phoneMapper;
    private final AesUtil aesUtil;
    private final PersonalWechatWeworkLinkService linkService;

    @Override
    public PageResult<PersonalWechatRespVO> list(String accountName, String wechatId, String status,
                                                 Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<PersonalWechatAccountDO> wrapper = new LambdaQueryWrapper<PersonalWechatAccountDO>()
                .eq(PersonalWechatAccountDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(accountName), PersonalWechatAccountDO::getAccountName, accountName)
                .like(StrUtil.isNotBlank(wechatId), PersonalWechatAccountDO::getWechatId, wechatId)
                .eq(StrUtil.isNotBlank(status), PersonalWechatAccountDO::getStatus, status)
                .orderByDesc(PersonalWechatAccountDO::getId);
        Page<PersonalWechatAccountDO> page = personalWechatAccountMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<PersonalWechatRespVO> list = page.getRecords().stream()
                .map(e -> toResp(e, true))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public PersonalWechatRespVO get(Long id) {
        return toResp(getRequiredInTenant(id), true);
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-personal-wechat", action = "create")
    public Long create(PersonalWechatCreateReq req) {
        Long tenantId = requireTenantId();
        assertWechatIdUnique(tenantId, req.getWechatId(), null);
        assertPhoneInTenant(req.getPhoneId(), tenantId);

        PersonalWechatAccountDO entity = new PersonalWechatAccountDO();
        entity.setTenantId(tenantId);
        entity.setAccountName(req.getAccountName());
        entity.setWechatId(req.getWechatId());
        entity.setContactPhone(normalizeContactPhone(req.getContactPhone()));
        entity.setPhoneId(req.getPhoneId());
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        personalWechatAccountMapper.insert(entity);
        if (req.getLinkedWeworkEmployeeId() != null) {
            linkService.syncLink(entity.getId(), req.getLinkedWeworkEmployeeId());
        }
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-personal-wechat", action = "update")
    public void update(PersonalWechatUpdateReq req) {
        PersonalWechatAccountDO existing = getRequiredInTenant(req.getId());
        Long tenantId = requireTenantId();
        if (StrUtil.isNotBlank(req.getWechatId()) && !req.getWechatId().equals(existing.getWechatId())) {
            assertWechatIdUnique(tenantId, req.getWechatId(), req.getId());
            existing.setWechatId(req.getWechatId());
        }
        if (StrUtil.isNotBlank(req.getAccountName())) {
            existing.setAccountName(req.getAccountName());
        }
        if (req.getContactPhone() != null) {
            existing.setContactPhone(normalizeContactPhone(req.getContactPhone()));
        }
        if (req.getPhoneId() != null) {
            assertPhoneInTenant(req.getPhoneId(), tenantId);
            existing.setPhoneId(req.getPhoneId());
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        personalWechatAccountMapper.updateById(existing);
        if (Boolean.TRUE.equals(req.getClearLinkedWeworkEmployee())) {
            linkService.syncLink(req.getId(), null);
        } else if (req.getLinkedWeworkEmployeeId() != null) {
            linkService.syncLink(req.getId(), req.getLinkedWeworkEmployeeId());
        }
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-personal-wechat", action = "delete")
    public void delete(Long id) {
        getRequiredInTenant(id);
        personalWechatAccountMapper.deleteById(id);
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-personal-wechat", action = "api-config")
    public void saveApiConfig(PersonalWechatApiConfigReq req) {
        PersonalWechatAccountDO existing = getRequiredInTenant(req.getId());
        if (StrUtil.isNotBlank(req.getApiUrl())) {
            existing.setApiUrlEncrypted(aesUtil.encrypt(req.getApiUrl()));
        }
        if (StrUtil.isNotBlank(req.getAppId())) {
            existing.setAppIdEncrypted(aesUtil.encrypt(req.getAppId()));
        }
        if (StrUtil.isNotBlank(req.getAppSecret())) {
            existing.setAppSecretEncrypted(aesUtil.encrypt(req.getAppSecret()));
        }
        if (StrUtil.isNotBlank(req.getToken())) {
            existing.setTokenEncrypted(aesUtil.encrypt(req.getToken()));
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        personalWechatAccountMapper.updateById(existing);
    }

    @Override
    public PersonalWechatRespVO getApiConfig(Long id) {
        PersonalWechatAccountDO entity = getRequiredInTenant(id);
        PersonalWechatRespVO vo = new PersonalWechatRespVO();
        vo.setId(entity.getId());
        vo.setApiUrl(maskIfPresent(entity.getApiUrlEncrypted()));
        vo.setAppId(maskIfPresent(entity.getAppIdEncrypted()));
        vo.setAppSecret(maskIfPresent(entity.getAppSecretEncrypted()));
        vo.setToken(maskIfPresent(entity.getTokenEncrypted()));
        return vo;
    }

    private PersonalWechatRespVO toResp(PersonalWechatAccountDO entity, boolean maskCredentials) {
        PersonalWechatRespVO vo = new PersonalWechatRespVO();
        vo.setId(entity.getId());
        vo.setAccountName(entity.getAccountName());
        vo.setWechatId(entity.getWechatId());
        vo.setContactPhone(entity.getContactPhone());
        vo.setPhoneId(entity.getPhoneId());
        vo.setPhoneNumberMasked(maskPhone(entity.getPhoneId()));
        vo.setStatus(entity.getStatus());
        if (entity.getCreateTime() != null) {
            vo.setCreateTime(entity.getCreateTime().format(DT_FMT));
        }
        if (maskCredentials) {
            vo.setApiUrl(maskIfPresent(entity.getApiUrlEncrypted()));
            vo.setAppId(maskIfPresent(entity.getAppIdEncrypted()));
            vo.setAppSecret(maskIfPresent(entity.getAppSecretEncrypted()));
            vo.setToken(maskIfPresent(entity.getTokenEncrypted()));
        }
        linkService.enrichPersonalWechat(vo, entity);
        return vo;
    }

    private String normalizeContactPhone(String contactPhone) {
        return StrUtil.blankToDefault(contactPhone, null);
    }

    private String maskIfPresent(String encrypted) {
        return StrUtil.isNotBlank(encrypted) ? MASK : null;
    }

    private String maskPhone(Long phoneId) {
        if (phoneId == null) {
            return null;
        }
        PhoneDO phone = phoneMapper.selectById(phoneId);
        if (phone == null || StrUtil.isBlank(phone.getPhoneNumberEncrypted())) {
            return null;
        }
        try {
            String plain = aesUtil.decrypt(phone.getPhoneNumberEncrypted());
            if (plain.length() >= 7) {
                return plain.substring(0, 3) + "****" + plain.substring(plain.length() - 4);
            }
            return MASK;
        } catch (Exception e) {
            return MASK;
        }
    }

    private PersonalWechatAccountDO getRequiredInTenant(Long id) {
        PersonalWechatAccountDO entity = personalWechatAccountMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!requireTenantId().equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private void assertWechatIdUnique(Long tenantId, String wechatId, Long excludeId) {
        LambdaQueryWrapper<PersonalWechatAccountDO> wrapper = new LambdaQueryWrapper<PersonalWechatAccountDO>()
                .eq(PersonalWechatAccountDO::getTenantId, tenantId)
                .eq(PersonalWechatAccountDO::getWechatId, wechatId);
        if (excludeId != null) {
            wrapper.ne(PersonalWechatAccountDO::getId, excludeId);
        }
        if (personalWechatAccountMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY);
        }
    }

    private void assertPhoneInTenant(Long phoneId, Long tenantId) {
        if (phoneId == null) {
            return;
        }
        PhoneDO phone = phoneMapper.selectById(phoneId);
        if (phone == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "关联手机不存在");
        }
        if (!tenantId.equals(phone.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return tenantId;
    }
}
