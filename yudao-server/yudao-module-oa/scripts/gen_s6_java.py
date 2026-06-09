#!/usr/bin/env python3
"""Generate GATE-S6 Java boilerplate (DO + Mapper)."""
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent / "src/main/java/cn/iocoder/yudao/module/oa"

DO_SPECS = [
    ("finance", "AccountCostDO", "oa_account_cost", [
        ("accountId", "Long"),
        ("costType", "String"),
        ("amount", "java.math.BigDecimal"),
        ("payMethod", "String"),
        ("payDate", "java.time.LocalDate"),
        ("period", "String"),
        ("remark", "String"),
        ("attachmentId", "Long"),
    ]),
    ("analytics", "FunnelDO", "oa_funnel", [
        ("funnelName", "String"),
        ("funnelType", "String"),
        ("status", "Integer"),
        ("remark", "String"),
    ]),
    ("analytics", "FunnelStepDO", "oa_funnel_step", [
        ("funnelId", "Long"),
        ("stepOrder", "Integer"),
        ("eventCode", "String"),
        ("stepName", "String"),
    ]),
    ("analytics", "CustomQueryDO", "oa_custom_query", [
        ("queryName", "String"),
        ("status", "String"),
        ("sqlText", "String"),
        ("paramsJson", "String"),
    ]),
    ("analytics", "DashboardDO", "oa_dashboard", [
        ("dashboardName", "String"),
        ("dashboardType", "String"),
        ("layoutJson", "String"),
        ("status", "Integer"),
    ]),
    ("analytics", "AccountStatusLogDO", "oa_account_status_log", [
        ("accountId", "Long"),
        ("statDate", "java.time.LocalDate"),
        ("status", "String"),
        ("followerCount", "Long"),
    ]),
    ("analytics", "ContentDailyDO", "oa_content_daily", [
        ("contentId", "Long"),
        ("statDate", "java.time.LocalDate"),
        ("readCount", "Long"),
        ("playCount", "Long"),
    ]),
    ("monitor", "ExternalWorkDO", "oa_external_work", [
        ("accountId", "Long"),
        ("platformType", "String"),
        ("title", "String"),
        ("workUrl", "String"),
        ("playCount", "Long"),
        ("completionRate", "java.math.BigDecimal"),
        ("likeCount", "Integer"),
        ("publishTime", "java.time.LocalDateTime"),
        ("industry", "String"),
        ("ipGroupId", "Long"),
        ("isExternal", "Integer"),
    ]),
]


def write_do(pkg, name, table, fields):
    tenant_ext = pkg != "analytics" or name not in ("FunnelStepDO",)
    extends = "TenantBaseDO" if tenant_ext else "object"
    if tenant_ext:
        header = f"""package cn.iocoder.yudao.module.oa.dal.dataobject.{pkg};

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
"""
        class_decl = f"@Data\n@EqualsAndHashCode(callSuper = true)\n@TableName(\"{table}\")\npublic class {name} extends TenantBaseDO {{\n"
    else:
        header = f"""package cn.iocoder.yudao.module.oa.dal.dataobject.{pkg};

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
"""
        class_decl = f"@Data\n@TableName(\"{table}\")\npublic class {name} {{\n\n    @TableId(type = IdType.AUTO)\n    private Long id;\n"

    body = ""
    for fname, ftype in fields:
        short = ftype.split(".")[-1]
        body += f"    private {short} {fname};\n"
    if not tenant_ext:
        body += """
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
"""
    footer = "}\n"
    path = BASE / "dal/dataobject" / pkg / f"{name}.java"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(header + "\n" + class_decl + body + footer, encoding="utf-8")


def write_mapper(pkg, do_name):
    mapper = do_name.replace("DO", "Mapper")
    content = f"""package cn.iocoder.yudao.module.oa.dal.mysql.{pkg};

import cn.iocoder.yudao.module.oa.dal.dataobject.{pkg}.{do_name};
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface {mapper} extends BaseMapper<{do_name}> {{
}}
"""
    path = BASE / "dal/mysql" / pkg / f"{mapper}.java"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def main():
    for pkg, name, table, fields in DO_SPECS:
        write_do(pkg, name, table, fields)
        write_mapper(pkg, name)
    print(f"Generated {len(DO_SPECS)} DO+Mapper pairs")


if __name__ == "__main__":
    main()
