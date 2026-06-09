package cn.iocoder.yudao.module.oa.controller.sop;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskCompleteReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskVO;
import cn.iocoder.yudao.module.oa.service.sop.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin-api/oa/task")
@Validated
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/list")
    public CommonResult<PageResult<TaskVO>> list(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long executorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(taskService.list(ipGroupId, status, executorId, startDate, endDate, pageNum, pageSize));
    }

    @GetMapping("/my-tasks")
    public CommonResult<PageResult<TaskVO>> myTasks(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(taskService.myTasks(pageNum, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody TaskCreateReq req) {
        return CommonResult.success(taskService.create(req));
    }

    @PostMapping("/{id}/start")
    public CommonResult<Boolean> start(@PathVariable Long id) {
        taskService.start(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/complete")
    public CommonResult<Boolean> complete(@PathVariable Long id,
                                          @RequestBody(required = false) TaskCompleteReq req) {
        taskService.complete(id, req);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/submit-review")
    public CommonResult<Boolean> submitReview(@PathVariable Long id) {
        taskService.submitReview(id);
        return CommonResult.success(true);
    }
}
