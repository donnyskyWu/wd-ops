package cn.iocoder.yudao.module.oa.service.sop;

import cn.iocoder.yudao.module.oa.api.dto.sop.SopReviewActionReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopReviewVO;

import java.util.List;

public interface SopReviewService {

    List<SopReviewVO> pending(Long reviewerId);

    void approve(SopReviewActionReq req);

    void reject(SopReviewActionReq req);
}
