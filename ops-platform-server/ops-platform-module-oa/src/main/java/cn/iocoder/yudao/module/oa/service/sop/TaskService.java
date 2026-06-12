package cn.iocoder.yudao.module.oa.service.sop;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskCompleteReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskVO;

import java.time.LocalDate;

public interface TaskService {

    PageResult<TaskVO> list(Long ipGroupId, String status, Long executorId,
                            LocalDate startDate, LocalDate endDate, Integer pageNum, Integer pageSize);

    PageResult<TaskVO> myTasks(Integer pageNum, Integer pageSize);

    Long create(TaskCreateReq req);

    void start(Long id);

    void complete(Long id, TaskCompleteReq req);

    void submitReview(Long id);
}
