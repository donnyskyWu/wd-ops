package cn.iocoder.yudao.module.oa.service.football;

import cn.iocoder.yudao.module.oa.dal.dataobject.football.AuthorArticleDO;
import cn.iocoder.yudao.module.oa.dal.mysql.football.AuthorArticleMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Member DB writes outside master {@code @Transactional} so {@code @DS("member")} routing works (ADR-051/054).
 */
@Service
@RequiredArgsConstructor
public class MemberArticleWriteService {

    private final AuthorArticleMapper authorArticleMapper;

    @DS("member")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void insert(AuthorArticleDO article) {
        authorArticleMapper.insert(article);
    }

    @DS("member")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AuthorArticleDO getById(Long id) {
        return authorArticleMapper.selectById(id);
    }

    @DS("member")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void updateById(AuthorArticleDO article) {
        authorArticleMapper.updateById(article);
    }
}
