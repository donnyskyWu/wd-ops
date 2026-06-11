package cn.iocoder.yudao.module.oa.controller.system;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.system.MessageSendReq;
import cn.iocoder.yudao.module.oa.api.dto.system.MessageVO;
import cn.iocoder.yudao.module.oa.service.system.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin-api/oa/system/message", "/admin-api/system/message"})
@Validated
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('oa:message:list')")
    public CommonResult<PageResult<MessageVO>> list(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String receiver,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(messageService.list(title, receiver, status, category, pageNo, pageSize));
    }

    @GetMapping("/get")
    @PreAuthorize("hasAuthority('oa:message:list')")
    public CommonResult<MessageVO> get(@RequestParam Long id) {
        return CommonResult.success(messageService.get(id));
    }

    @GetMapping("/unread")
    @PreAuthorize("hasAuthority('oa:message:inbox')")
    public CommonResult<PageResult<MessageVO>> unread(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(messageService.listUnread(pageNo, pageSize));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('oa:message:inbox')")
    public CommonResult<Long> unreadCount() {
        return CommonResult.success(messageService.unreadCount());
    }

    @PutMapping("/read")
    @PreAuthorize("hasAuthority('oa:message:inbox')")
    public CommonResult<Boolean> markRead(@RequestParam Long id) {
        messageService.markRead(id);
        return CommonResult.success(true);
    }

    @PostMapping("/send")
    @PreAuthorize("hasAuthority('oa:message:send')")
    public CommonResult<Long> send(@Valid @RequestBody MessageSendReq req) {
        return CommonResult.success(messageService.send(req));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('oa:message:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        messageService.delete(id);
        return CommonResult.success(true);
    }
}
