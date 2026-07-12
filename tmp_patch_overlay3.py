from pathlib import Path
p = Path(r"d:/self/sy/运营数据平台/202606/wd/scripts/integration-config/football-integration-overlay.yml")
text = p.read_text(encoding="utf-8")
if "RocketMQAutoConfiguration" not in text:
    text = text.replace(
        "spring:\n  cloud:",
        "spring:\n  autoconfigure:\n    exclude:\n      - org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration\n  cloud:",
        1,
    )
p.write_text(text, encoding="utf-8")
print('restored mq exclude')
