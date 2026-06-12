package cn.iocoder.yudao.module.oa.service.system;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.system.MessageSendReq;
import cn.iocoder.yudao.module.oa.api.dto.system.MessageVO;

public interface MessageService {

    PageResult<MessageVO> list(String title, String receiver, String status, String category,
                               Integer pageNo, Integer pageSize);

    Long send(MessageSendReq req);

    void delete(Long id);

    MessageVO get(Long id);

    PageResult<MessageVO> listUnread(Integer pageNo, Integer pageSize);

    Long unreadCount();

    void markRead(Long id);
}
