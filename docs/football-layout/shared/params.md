# 排版参数与 NL 调整（两模板共用）

## 字体参数字典

| 参数名 | 类型 | 说明 |
|--------|------|------|
| `title_font_size` | px | 文章标题字号 |
| `title_color` | hex | 文章标题颜色 |
| `match_header_font_size` | px | 比赛头字号 |
| `match_header_color` | hex | 比赛头颜色 |
| `team_vs_font_size` | px | 对阵行字号 |
| `team_vs_color` | hex | 队名颜色 |
| `vs_text_color` | hex | "VS"文字颜色 |
| `match_time_font_size` | px | 比赛时间字号 |
| `match_time_color` | hex | 比赛时间颜色 |
| `paragraph_font_size` | px | 正文字号 |
| `paragraph_line_height` | em | 正文行高 |
| `paragraph_color` | hex | 正文颜色 |
| `rec_label_font_size` | px | 推荐标签字号 |
| `rec_label_color` | hex | 推荐标签颜色 |
| `rec_value_font_size` | px | 推荐值字号 |
| `rec_value_color` | hex | 推荐值颜色（比分/指数） |
| `rec_highlight_color` | hex | 推荐胜负值文字颜色 |
| `rec_highlight_background` | hex | 推荐胜负值背景高亮色 |
| `rec_conclusion_font_size` | px | 预测结论字号 |
| `subheading_font_size` | px | 小标题字号 |
| `subheading_color` | hex | 小标题颜色 |
| `footer_font_size` | px | 页脚字号（最小允许值） |
| `footer_color` | hex | 页脚颜色 |
| `image_placeholder_font_size` | px | 图片占位文字号 |
| `image_placeholder_color` | hex | 图片占位字颜色 |
| `toolbar_background_color` | hex/rgba | 顶部标题栏背景色 |
| `toolbar_title_color` | hex | 标题栏标题颜色 |

## 间距参数字典

| 参数名 | 默认值（px） | 说明 |
|--------|-------------|------|
| `paragraph_margin_bottom` | 16（decision-scan）/ 10（默认） | 段落底部间距 |
| `paragraph_text_indent` | 0（decision-scan）/ 2em（analysis-report） | 正文首行缩进 |
| `paragraph_margin_top` | 0 | 段落顶部间距 |
| `rec_item_margin_bottom` | 6 | 推荐列表项间距 |
| `rec_block_margin_top` | 12 | 推荐区块顶部间距 |
| `block_margin_top` | 30 | 比赛Block间间距 |
| `block_padding_top` | 30 | 比赛Block内顶部内边距 |
| `section_margin_top` | 20 | 章节标题上方间距 |
| `title_margin_bottom` | 16 | 标题底部间距 |
| `block_separator_color` | #d8dbe4 | 分隔线颜色 |

## 自然语言 → 参数映射

### 匹配模式（按优先级排列，命中即止）

| 用户模式 | 匹配规则 | 动作 | 示例 |
|---------|---------|------|------|
| `<元素>改成/用/变成/调成/设为 <值>` | 元素名定位 + 值 | 更新对应参数 | 标题用蓝色、正文调成14px |
| `<元素>颜色改成/变为 <颜色>` | 元素名 + 颜色触发词 | 更新 color | 标题颜色改成蓝色 |
| `<元素>加粗` | 元素名 + 加粗 | 更新 font_weight = bold | 标题加粗 |
| `<元素>不加粗/变细` | 元素名 + 不加粗 | 更新 font_weight = normal | 推荐值不加粗 |
| `间距加大/大一点/增大` | 间距触发词 + 幅度 | 所有 margin × 1.3 | 间距加大 |
| `间距减小/小一点/缩小/减小` | 间距触发词 + 幅度 | 所有 margin × 0.7 | 间距小一点 |
| `紧凑/紧密/收紧` | 紧凑触发词 | margin × 0.6 + paragraph_margin_bottom=6 | 排版紧凑 |
| `宽松/舒展开/松一些` | 宽松触发词 | margin × 1.5 | 宽松一点 |
| `主题色改成/换成/变为 <颜色>` | 主题色触发词 | 所有主题色统一替换 | 主题色改成绿色 |
| `标题栏改成/设为 <颜色>` | 标题栏触发词 | 更新 toolbar_background_color | 标题栏改成浅灰 |
| `正文行高改成/设为 <值>` | 行高触发词 | 更新 line_height | 行高改成2.0 |
| `<元素>改成 <N>px 或 N-Npx` | 字号 + px 后缀 | 更新 font_size | 正文16px、标题22-24px |
| `<范围>都/全/一起 <调整>` | 范围词 + 调整 | 批量应用 | 所有字号加大2px |
| `恢复默认/还原/重置` | 重置触发词 | 全部参数恢复出厂值 | 恢复默认 |
| `切换到/换到/改用 <模板名>` | 模板切换触发词 | 切换模板并重新生成 | 切换到情报分析版 |

### 模板切换映射

| 用户说法 | 目标模板文件 | 说明 |
|---------|-------------|------|
| 决策扫读版/扫读版/精简版/模板一 | `template-decision-scan.md` | 决策路径优化 |
| 情报分析版/分析版/报告版/深度版/模板二 | `template-analysis-report.md` | 深度报告风格，数据可视化 |
| 切换模板/换模板 | 提示可选模板列表 | 让用户选择 |

### 失败处理协议

```
IF 用户语句无匹配参数
  → 提示可调参数。包括：字号、颜色、间距、行高、加粗/不加粗、切换模板、恢复默认

IF 元素名无法识别
  → "未识别到排版元素「{元素名}」。已知元素：标题、比赛头、对阵、时间、正文、推荐标签、推荐值、小标题、页脚、图片说明。"

IF 值格式错误（如颜色名非标准）
  → 提示十六进制色号或标准颜色名

IF 用户输入"帮助/可以调什么/帮我看看"
  → 显示可调参数概览 → 询问需要调整哪部分

IF 模板名无法识别
  → "未识别到模板「{模板名}」。可用模板：决策扫读版、情报分析版。"
```

### 多命令拆分规则

```
一次含多个调整命令时：
1. 按换行或"、"、"；"拆分为原子操作
2. 依次执行每个原子操作
3. 执行完全部原子操作后统一重新生成 HTML
4. 反馈时逐条告知变更

例：用户"标题用蓝色，正文16px，间距紧凑"
→ 拆分为: [title_color=#1a2a4a, paragraph_font_size=16, margin×0.6]
→ 反馈：已同步应用 3 项调整：
   - 标题颜色改为深蓝 #1a2a4a
   - 正文改为 16px
   - 间距收紧至默认的 60%
```
