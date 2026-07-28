package cn.iocoder.yudao.framework.common.biz.member.article;

import cn.iocoder.yudao.framework.common.biz.member.article.dto.ArticleSaveDTO;
import cn.iocoder.yudao.framework.common.biz.member.article.dto.ArticleStatusChangeDTO;
import cn.iocoder.yudao.framework.common.enums.RpcConstants;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Football member-server article write RPC (vendored subset, G-MEM-03).
 */
@FeignClient(name = RpcConstants.MEMBER_NAME, primary = false)
public interface ArticleApi {

    String PREFIX = RpcConstants.MEMBER_PREFIX + "/article";

    @PostMapping(PREFIX + "/create")
    CommonResult<Long> createArticle(@RequestBody ArticleSaveDTO dto);

    @PutMapping(PREFIX + "/update")
    CommonResult<Boolean> updateArticle(@RequestBody ArticleSaveDTO dto);

    @PostMapping(PREFIX + "/status-change")
    CommonResult<Boolean> statusChange(@RequestBody ArticleStatusChangeDTO dto);
}
