package cn.iocoder.yudao.module.oa.controller.operations;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerStatsVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerTrendVO;
import cn.iocoder.yudao.module.oa.service.operations.FollowerAnalysisService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/admin-api/oa/follower-analysis")
@Validated
@RequiredArgsConstructor
public class FollowerAnalysisController {

    private final FollowerAnalysisService followerAnalysisService;

    @GetMapping("/list")
    public CommonResult<PageResult<FollowerAnalysisVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String platformType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return CommonResult.success(followerAnalysisService.list(startDate, endDate, ipGroupId, accountId, platformType, page, size));
    }

    @GetMapping("/trend")
    public CommonResult<List<FollowerTrendVO>> trend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String dimension) {
        return CommonResult.success(followerAnalysisService.trend(startDate, endDate, ipGroupId, accountId));
    }

    /**
     * S-R6-B1+B4：聚合统计（spec AC-M1-004-1 关键指标卡）
     * 前端 4 KPI 卡（粉丝总数/新增/取消/净增/增长率）从本页 list 改为读 /stats 端点。
     */
    @GetMapping("/stats")
    public CommonResult<FollowerStatsVO> stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) String dimension) {
        return CommonResult.success(followerAnalysisService.stats(startDate, endDate, ipGroupId, accountId, platformType, dimension));
    }

    /**
     * S-R6-B3：粉丝分析导出（spec AC-M1-004-2）
     * 文件名 follower_analysis_{yyyyMMddHHmmss}.csv（注：spec 原文 xlsx，但 oa 模块暂未引入 POI，
     * 暂以 csv 输出 + UTF-8 BOM 兼容 Excel；spec 同步说明见 S-R6-报告）。
     */
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) String dimension,
            HttpServletResponse response) throws IOException {
        byte[] bytes = followerAnalysisService.exportCsv(startDate, endDate, ipGroupId, accountId, platformType, dimension);
        String filename = "follower_analysis_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        try (OutputStream os = response.getOutputStream()) {
            os.write(bytes);
            os.flush();
        }
    }
}
