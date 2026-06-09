package cn.iocoder.yudao.module.oa.api.dto.sop;

import lombok.Data;

import java.util.List;

@Data
public class DagValidateResp {

    private boolean valid;
    private List<Long> cyclePath;

    public static DagValidateResp ok() {
        DagValidateResp resp = new DagValidateResp();
        resp.setValid(true);
        return resp;
    }

    public static DagValidateResp cycle(List<Long> cyclePath) {
        DagValidateResp resp = new DagValidateResp();
        resp.setValid(false);
        resp.setCyclePath(cyclePath);
        return resp;
    }
}
