package cn.iocoder.yudao.framework.common.biz.member.author;

import cn.iocoder.yudao.framework.common.biz.member.author.dto.AuthorSimpleRespDTO;
import cn.iocoder.yudao.framework.common.enums.RpcConstants;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Football member-server author read RPC (vendored subset, G-MEM-02).
 */
@FeignClient(contextId = "authorApi", name = RpcConstants.MEMBER_NAME, primary = false)
public interface AuthorApi {

    String PREFIX = RpcConstants.MEMBER_PREFIX + "/author";

    @GetMapping(PREFIX + "/getAuthor")
    CommonResult<AuthorSimpleRespDTO> getAuthor(@RequestParam("id") Long id);

    @GetMapping(PREFIX + "/getAuthors")
    CommonResult<List<AuthorSimpleRespDTO>> getAuthors(@RequestParam("ids") Collection<Long> ids);

    @GetMapping(PREFIX + "/listByAuthorIds")
    CommonResult<List<AuthorSimpleRespDTO>> listByAuthorIds(@RequestParam("ids") Set<Long> ids);

    @GetMapping(PREFIX + "/getAuthorByUserId")
    CommonResult<AuthorSimpleRespDTO> getAuthorByUserId(@RequestParam("userId") Long userId);
}
