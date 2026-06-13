package cn.iocoder.yudao.module.oa.api.dto.match;

import lombok.Data;

@Data
public class MatchVO {

    /** 外部 scheduleId，计划/任务 competitionId 使用该值 */
    private String scheduleId;
    /** 展示名：联赛-主队 VS 客队-时间 */
    private String displayName;
    private String sClassId;
    private String sClassName;
    private String homeTeamName;
    private String guestTeamName;
    /** 格式化比赛时间 yyyy-MM-dd HH:mm */
    private String matchTime;
    private Long matchTimeRaw;
    /** 竞彩类型，如 jc */
    private String lotteryType;
}
