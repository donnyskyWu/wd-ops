from pathlib import Path
p = Path(r"d:/self/sy/运营数据平台/202606/wd/scripts/integration-config/football-integration-overlay.yml")
text = p.read_text(encoding="utf-8")
text = text.replace(
    "  autoconfigure:\n    exclude:\n      - org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration\n",
    "",
)
text = text.replace(
    "rocketmq:\n  name-server: 192.168.10.51:9876",
    "rocketmq:\n  name-server: 127.0.0.1:9876",
)
p.write_text(text, encoding="utf-8")
print('overlay updated')
