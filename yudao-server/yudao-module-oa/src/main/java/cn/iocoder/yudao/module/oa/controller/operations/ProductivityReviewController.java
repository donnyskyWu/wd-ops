package cn.iocoder.yudao.module.oa.controller.operations;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.ProductivityReviewDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ProductivityReviewVO;
import cn.iocoder.yudao.module.oa.service.operations.ProductivityReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin-api/oa/productivity-review")
@Validated
@RequiredArgsConstructor
public class ProductivityReviewController {

    private final ProductivityReviewService productivityReviewService;

    // S-R9 B1: 加 4 参（startDate/endDate/timeDimension/position/keyword）
    // S-R9 B9: 统一 page/size
    @GetMapping("/list")
    public CommonResult<PageResult<ProductivityReviewVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "WEEK") String timeDimension,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return CommonResult.success(productivityReviewService.list(
                startDate, endDate, timeDimension, ipGroupId, userId, position, keyword, page, size));
    }

    // S-R9 B3: 4 Card 详情
    @GetMapping("/detail/{userId}")
    public CommonResult<ProductivityReviewDetailVO> detail(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(productivityReviewService.detail(userId, startDate, endDate));
    }

    // S-R9 B3: 主播详情（IP 组下所有账号）
    @GetMapping("/detail/anchors")
    public CommonResult<List<ProductivityReviewVO>> anchors(
            @RequestParam Long ipGroupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(productivityReviewService.anchors(ipGroupId, startDate, endDate));
    }

    // S-R9 B3: 导出（同步 CSV）
    @PostMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "WEEK") String timeDimension,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String keyword) {
        String csv = productivityReviewService.exportCsv(startDate, endDate, timeDimension, ipGroupId, userId, position, keyword);
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=utf-8"));
        headers.setContentDispositionFormData("attachment", "productivity_review.csv");
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
