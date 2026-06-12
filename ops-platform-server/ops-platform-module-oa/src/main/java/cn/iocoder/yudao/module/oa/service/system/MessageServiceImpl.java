package cn.iocoder.yudao.module.oa.service.system;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.system.MessageSendReq;
import cn.iocoder.yudao.module.oa.api.dto.system.MessageVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.system.SysMessageDO;
import cn.iocoder.yudao.module.oa.dal.mysql.system.SysMessageMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysMessageMapper sysMessageMapper;

    @Override
    public PageResult<MessageVO> list(String title, String receiver, String status, String category,
                                      Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysMessageDO> wrapper = new LambdaQueryWrapper<SysMessageDO>()
                .eq(SysMessageDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(title), SysMessageDO::getTitle, title)
                .like(StrUtil.isNotBlank(receiver), SysMessageDO::getReceiver, receiver)
                .eq(StrUtil.isNotBlank(status), SysMessageDO::getStatus, status)
                .eq(StrUtil.isNotBlank(category), SysMessageDO::getCategory, category)
                .orderByDesc(SysMessageDO::getId);
        Page<SysMessageDO> page = sysMessageMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<MessageVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-message", action = "send")
    public Long send(MessageSendReq req) {
        Long tenantId = requireTenantId();
        SysMessageDO row = new SysMessageDO();
        row.setTenantId(tenantId);
        row.setTitle(req.getTitle());
        row.setCategory(req.getCategory());
        row.setChannel(String.join(",", req.getChannels()));
        row.setReceiver(req.getReceiver());
        row.setContent(req.getContent());
        row.setStatus("SENT");
        row.setSendTime(LocalDateTime.now());
        row.setCreator(TenantContextHolder.getUsername());
        row.setUpdater(TenantContextHolder.getUsername());
        sysMessageMapper.insert(row);
        return row.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-message", action = "delete")
    public void delete(Long id) {
        Long tenantId = requireTenantId();
        SysMessageDO row = sysMessageMapper.selectById(id);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        sysMessageMapper.deleteById(id);
    }

    @Override
    public MessageVO get(Long id) {
        Long tenantId = requireTenantId();
        SysMessageDO row = sysMessageMapper.selectById(id);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        return toVO(row);
    }

    @Override
    public PageResult<MessageVO> listUnread(Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysMessageDO> wrapper = currentUserInboxWrapper(tenantId)
                .isNull(SysMessageDO::getReadTime)
                .orderByDesc(SysMessageDO::getSendTime)
                .orderByDesc(SysMessageDO::getId);
        Page<SysMessageDO> page = sysMessageMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<MessageVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public Long unreadCount() {
        Long tenantId = requireTenantId();
        return sysMessageMapper.selectCount(currentUserInboxWrapper(tenantId).isNull(SysMessageDO::getReadTime));
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-message", action = "mark-read")
    public void markRead(Long id) {
        Long tenantId = requireTenantId();
        SysMessageDO row = sysMessageMapper.selectById(id);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!isCurrentUserReceiver(row)) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN.getCode(), "无权查看该消息");
        }
        if (row.getReadTime() == null) {
            row.setReadTime(LocalDateTime.now());
            row.setUpdater(TenantContextHolder.getUsername());
            row.setUpdateTime(LocalDateTime.now());
            sysMessageMapper.updateById(row);
        }
    }

    private MessageVO toVO(SysMessageDO row) {
        MessageVO vo = new MessageVO();
        vo.setId(row.getId());
        vo.setTitle(row.getTitle());
        vo.setCategory(row.getCategory());
        vo.setChannel(row.getChannel());
        vo.setReceiver(row.getReceiver());
        vo.setContent(row.getContent());
        vo.setStatus(row.getStatus());
        vo.setFailReason(row.getFailReason());
        if (row.getSendTime() != null) {
            vo.setSendTime(row.getSendTime().format(FMT));
        }
        if (row.getReadTime() != null) {
            vo.setReadTime(row.getReadTime().format(FMT));
        }
        vo.setRead(row.getReadTime() != null);
        return vo;
    }

    private LambdaQueryWrapper<SysMessageDO> currentUserInboxWrapper(Long tenantId) {
        Set<String> receivers = currentUserReceivers();
        LambdaQueryWrapper<SysMessageDO> wrapper = new LambdaQueryWrapper<SysMessageDO>()
                .eq(SysMessageDO::getTenantId, tenantId)
                .eq(SysMessageDO::getStatus, "SENT");
        wrapper.and(w -> {
            boolean first = true;
            for (String receiver : receivers) {
                if (first) {
                    w.eq(SysMessageDO::getReceiver, receiver);
                    first = false;
                } else {
                    w.or().eq(SysMessageDO::getReceiver, receiver);
                }
            }
        });
        return wrapper;
    }

    private boolean isCurrentUserReceiver(SysMessageDO row) {
        return currentUserReceivers().contains(row.getReceiver());
    }

    private Set<String> currentUserReceivers() {
        LoginUser user = LoginUserContext.getRequired();
        Set<String> receivers = new LinkedHashSet<>();
        addIfNotBlank(receivers, user.getUsername());
        addIfNotBlank(receivers, user.getNickname());
        addIfNotBlank(receivers, user.getEmail());
        addIfNotBlank(receivers, String.valueOf(user.getUserId()));
        return receivers;
    }

    private void addIfNotBlank(Set<String> values, String value) {
        if (StrUtil.isNotBlank(value)) {
            values.add(value);
        }
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return tenantId;
    }
}
