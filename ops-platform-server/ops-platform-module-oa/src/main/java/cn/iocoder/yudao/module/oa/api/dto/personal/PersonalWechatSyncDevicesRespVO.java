package cn.iocoder.yudao.module.oa.api.dto.personal;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PersonalWechatSyncDevicesRespVO {

    private int autoBoundCount;
    private int updatedSnapshotCount;
    private int pendingCount;
    private List<AochuangPendingDeviceVO> pendingDevices = new ArrayList<>();
}
