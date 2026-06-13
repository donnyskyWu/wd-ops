package cn.iocoder.yudao.module.oa.service.sop;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskAttachmentVO;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskCompleteReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskExecuteSaveReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskExecuteVO;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskVO;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface TaskService {

    PageResult<TaskVO> list(Long ipGroupId, String status, Long executorId,
                            LocalDate startDate, LocalDate endDate, Integer pageNum, Integer pageSize);

    PageResult<TaskVO> myTasks(Integer pageNum, Integer pageSize);

    List<TaskVO> listTasksForPlan(Long planId);

    Long create(TaskCreateReq req);

    void start(Long id);

    void complete(Long id, TaskCompleteReq req);

    void submitReview(Long id);

    TaskExecuteVO getExecuteContext(Long id);

    void saveExecute(Long id, TaskExecuteSaveReq req);

    TaskAttachmentVO uploadExecuteAttachment(Long id, MultipartFile file);

    void executeComplete(Long id);
}
