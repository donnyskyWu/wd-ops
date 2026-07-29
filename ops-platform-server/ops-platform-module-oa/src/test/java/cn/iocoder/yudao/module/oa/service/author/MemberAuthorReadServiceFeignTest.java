package cn.iocoder.yudao.module.oa.service.author;

import cn.iocoder.yudao.framework.common.biz.member.author.AuthorApi;
import cn.iocoder.yudao.framework.common.biz.member.author.dto.AuthorSimpleRespDTO;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorUserDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberAuthorReadServiceFeignTest {

    @Mock
    private AuthorApi authorApi;

    private MemberAuthorReadService service;

    @BeforeEach
    void setUp() {
        service = new MemberAuthorReadService(authorApi);
    }

    @Test
    @DisplayName("G-MEM-02: loadByIds 走 AuthorApi.getAuthors")
    void loadByIdsViaFeign() {
        AuthorSimpleRespDTO dto = new AuthorSimpleRespDTO();
        dto.setId(1001L);
        dto.setNickname("作者甲");
        dto.setTenantId(1L);
        dto.setStatus(0);
        when(authorApi.getAuthors(List.of(1001L))).thenReturn(CommonResult.success(List.of(dto)));

        Map<Long, AuthorUserDO> map = service.loadByIds(List.of(1001L));

        assertEquals(1, map.size());
        assertEquals("作者甲", map.get(1001L).getNickname());
    }
}
