# 情报分析版模板（`analysis-report`）

> 适用场景：深度分析报告，品牌感强，自带数据可视化（统计条/赔率表/对阵卡）。所有样式内联，复制到编辑器可完整保留格式。

## 设计意图

### 核心模型：深度阅读者

读者行为是"情报分析师"模式——他们需要在一份报告中同时获取：赛事档案、基本面数据、战术分析、赔率解读、胜负推演。

```
阅读路径：
品牌认知（作者+赛事）→ 赛事档案（背景确认）→ 基本面分析（信源评估）
                                              ↓
                                赔率验证（市场态度）→ 结论判决（最终决策）
```

### 生态适配

| 生态约束 | 设计决定 |
|---------|---------|
| 直接浏览器预览/截图分享 | 组件自带精致样式 |
| 品牌展示 | 渐变深色头部 + Brand Badge |
| 数据可视化 | 内联 CSS 统计条、赔率表格、VS 对阵卡 |
| 手机阅读 | max-width: 720px |
| 跨平台 | PingFang / Microsoft YaHei |
| **复制到编辑器保留格式** | **所有 content 内元素使用内联 style，`::before` 用 `<span>` 替代** |

## 色彩系统

| 颜色 | 排版功能 |
|------|---------|
| 深渐变 `#1a1a2e→#0f3460` | 头部 Banner 背景（品牌色） |
| 红色 `#e94560` | 强调色：数字标号、VS、统计条、品牌 Badge 渐变 |
| 深蓝 `#0f3460` | 辅助色：统计条、dot 标识 |
| 浅灰 `#f8f9fa` | 卡片/信息区背景 |
| 正文 `#444` | 段落文字 |
| 中灰 `#888` | 降级信息/标签 |
| 白色文字 `#fff` | 深色底上的文字 |
| 浅色文字 `#a0aec0` | 头部的辅助信息文字 |
| 黄底 `#fff5f5` | 高亮框背景 |
| 免责 `#ffedd5` | 免责声明底色 |

## 组件库

| 组件 | 标签 | 适用内容 |
|------|------|---------|
| 头部 Banner | `<div>` 渐变深色底 | 品牌 + 标题 + 标签 |
| 品牌 Badge | `<div>` 圆角渐变标签 | "阿豹料码"等作者品牌 |
| 文章主标题 | `<h1>` | 赛事编号 + 对阵 |
| 副标题 | `<div>` subtitle | 赛事阶段/轮次 |
| 比赛标签 | `<div>` match-tag | 时间 + 场地 |
| 章节标题 | `<div>` section-title | 带编号红色圆圈的段落标题 |
| 信息行 | `<div>` match-info-row | 键值对信息（flex space-between） |
| 对阵 VS 卡 | `<div>` teams-box | 对阵双方 + 联赛信息 |
| 高亮框 | `<div>` highlight-box | 红色左边框 + 浅红底 |
| 分析卡片 | `<div>` analysis-card | 浅灰底 + 圆角 + 点状小标题 |
| 数据统计条 | `<div>` stat-bar | 双色进度条对比 |
| 赔率表格 | `<table>` | 多家机构赔率对比 |
| 预测框 | `<div>` prediction-box | 深色渐变底 + 推荐结果 + 比分 |
| 预测比分徽章 | `<div>` score-badge | 半透明圆角卡片 |
| 免责声明 | `<div>` disclaimer | 黄底免责文本 |
| 分割线 | 自身 margin | section 之间 32px |

### 组件选择逻辑

```
原文有？
├─ 作者品牌 → 头部 Banner + Brand Badge
├─ 赛事编号 + 对阵 → h1 + 副标题 + match-tag
├─ 键值对赛事信息 → match-info-row（flex 行）
├─ 对阵双方 → teams-box（flex 左右列 + VS 居中）
├─ 重点强调 → highlight-box（红左边框）
├─ 子分析段落 → analysis-card（灰底 + 小标题 + 列表）
├─ 数据对比 → stat-bar（双色进度条）
├─ 赔率 → odds-table（表格）
├─ 最终预测 → prediction-box（深色渐变 + 比分徽章）
└─ 免责声明 → disclaimer 条款
```

## 排版参数默认值

### 字体

| 参数名 | 默认值 | 说明 |
|--------|-------|------|
| title_font_size | 24 | 文章标题字号（h1），px |
| title_color | #ffffff | 文章标题颜色 |
| section_title_size | 18 | 章节标题字号，px |
| section_title_color | #1a1a2e | 章节标题颜色 |
| paragraph_font_size | 15 | 正文字号，px |
| paragraph_line_height | 1.8 | 正文行高 |
| paragraph_color | #444 | 正文颜色 |
| team_name_size | 16 | 队名字号，px |
| team_name_color | #1a1a2e | 队名颜色 |
| vs_size | 20 | VS 字号，px |
| vs_color | #e94560 | VS 颜色 |
| info_label_color | #888 | 信息标签颜色 |
| info_value_color | #333 | 信息值颜色 |
| card_bg | #f8f9fa | 卡片背景色 |
| highlight_border | #e94560 | 高亮框左边框色 |
| stat_left_color | #e94560 | 统计条左侧色 |
| stat_right_color | #0f3460 | 统计条右侧色 |
| brand_bg_start | #e94560 | 品牌 Badge 渐变起始 |
| brand_bg_end | #ff6b6b | 品牌 Badge 渐变结束 |
| header_bg_start | #1a1a2e | 头部渐变起始 |
| header_bg_mid | #16213e | 头部渐变中间 |
| header_bg_end | #0f3460 | 头部渐变结束 |
| prediction_bg_start | #1a1a2e | 预测框渐变起始 |
| prediction_bg_end | #0f3460 | 预测框渐变结束 |
| prediction_result_color | #ff6b6b | 预测结果颜色 |
| prediction_label_color | #a0aec0 | 预测框标签颜色 |
| score_value_color | #fff | 比分徽章值颜色 |
| footer_font_size | 13 | 页脚字号，px |
| footer_color | #8a8a98 | 页脚颜色 |
| disclaimer_bg | #fffde7 | 免责背景色 |
| toolbar_background_color | rgba(255,255,255,0.92) | 标题栏背景 |
| toolbar_title_color | #1a2a4a | 标题栏标题颜色 |

### 间距

| 参数名 | 默认值（px） |
|--------|-------------|
| paragraph_margin_bottom | 12 |
| section_margin_bottom | 32 |
| section_title_margin_bottom | 16 |
| header_padding | 40px 30px 30px |
| content_padding | 30px |
| card_padding | 20px |
| card_margin_bottom | 16px |
| highlight_margin | 16px 0 |
| prediction_margin | 20px 0 |
| block_margin_top | 30 |

## HTML 模板

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>{文章标题}</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body {
    font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
    background: #f5f5f5; color: #333; line-height: 1.8; padding: 20px;
    padding-top: 84px;
  }
  .container {
    max-width: 720px; margin: 0 auto; background: #fff;
    border-radius: 12px; overflow: hidden;
    box-shadow: 0 2px 20px rgba(0,0,0,0.08);
  }
  .content { padding: 30px; }

  #toolbar {
    position: fixed; top: 0; left: 0; right: 0; z-index: 9999;
    background: rgba(255,255,255,0.92);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    border-bottom: 1px solid rgba(0,0,0,0.04);
  }
  #toolbar-inner {
    display: flex; justify-content: space-between; align-items: center;
    max-width: 720px; margin: 0 auto; padding: 14px 20px;
  }
  #toolbar-title {
    font-size: 15px; font-weight: 500; color: #1a2a4a;
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    flex: 1; margin-right: 12px; letter-spacing: 0.5px; opacity: 0.8;
  }
  #copy-btn {
    background: #1a2a4a; color: #fff; border: none; border-radius: 6px;
    padding: 10px 22px; font-size: 13px; font-weight: 500;
    cursor: pointer; white-space: nowrap; flex-shrink: 0;
    transition: all 0.35s cubic-bezier(0.25,0.46,0.45,0.94);
    letter-spacing: 0.8px;
  }
  #copy-btn.copied { background: #2d7d5a; }
  #toast {
    position: fixed; bottom: 36px; left: 50%; transform: translateX(-50%);
    background: rgba(26,26,46,0.88); color: #f0f0f0; padding: 10px 24px;
    border-radius: 8px; font-size: 13px; z-index: 99999;
    opacity: 0; transition: opacity 0.3s ease; pointer-events: none;
    white-space: nowrap;
  }
  #toast.show { opacity: 1; }
</style>
</head>
<body>
  <div id="toolbar">
    <div id="toolbar-inner">
      <span id="toolbar-title">{文章标题}</span>
      <button id="copy-btn" onclick="copyFormatted()">复制全文</button>
    </div>
  </div>
  <div class="container">
    <div style="background:linear-gradient(135deg,#1a1a2e 0%,#16213e 50%,#0f3460 100%);color:#fff;padding:40px 30px 30px;text-align:center;font-family:'PingFang SC','Microsoft YaHei','Helvetica Neue',Arial,sans-serif;">
      <span style="display:inline-block;background:linear-gradient(90deg,#e94560,#ff6b6b);color:#fff;font-size:14px;font-weight:bold;padding:4px 16px;border-radius:20px;margin-bottom:16px;letter-spacing:2px;font-family:'PingFang SC','Microsoft YaHei','Helvetica Neue',Arial,sans-serif;">{作者品牌}</span>
      <h1 style="font-size:24px;margin-bottom:8px;line-height:1.4;color:#fff;font-family:'PingFang SC','Microsoft YaHei','Helvetica Neue',Arial,sans-serif;">{赛事编号 - 赛事名称}<br>{主队 VS 客队}</h1>
      <div style="font-size:15px;color:#a0aec0;margin-top:6px;font-family:'PingFang SC','Microsoft YaHei','Helvetica Neue',Arial,sans-serif;">{赛事阶段}</div>
      <span style="display:inline-block;background:rgba(255,255,255,0.1);border:1px solid rgba(255,255,255,0.2);border-radius:6px;padding:4px 12px;font-size:13px;color:#e2e8f0;margin-top:12px;font-family:'PingFang SC','Microsoft YaHei','Helvetica Neue',Arial,sans-serif;">{时间} · {场地}</span>
    </div>
    <div class="content">
      <!-- 解析后的内容逐元素填充 -->
    </div>
  </div>
  <div id="toast"></div>
  <script>
    function copyFormatted() {
      var btn = document.getElementById('copy-btn');
      var el = document.querySelector('.container');
      var html = el.innerHTML;
      var ta = document.createElement('textarea');
      ta.value = html;
      ta.style.cssText = 'position:fixed;top:-9999px;left:-9999px;width:1px;height:1px;opacity:0;';
      document.body.appendChild(ta);
      ta.focus();
      ta.select();
      var ok = false;
      try { ok = document.execCommand('copy'); } catch (e) {}
      document.body.removeChild(ta);
      if (ok) {
        btn.textContent = '已复制';
        btn.classList.add('copied');
        clearTimeout(btn._resetTimer);
        btn._resetTimer = setTimeout(function() {
          btn.textContent = '复制全文';
          btn.classList.remove('copied');
        }, 2000);
        showToast('已复制 HTML 源码，在编辑器源代码模式粘贴');
      } else {
        showToast('复制失败，请手动选中后复制');
      }
    }
    function showToast(msg) {
      var t = document.getElementById('toast');
      t.textContent = msg;
      t.classList.add('show');
      clearTimeout(t._timer);
      t._timer = setTimeout(function() { t.classList.remove('show'); }, 2000);
    }
  </script>
</body>
</html>
```

## 组件渲染规则

### 头部 Banner
- 渐变深色底：`linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)`
- Brand Badge：圆角 20px，红渐变 `linear-gradient(90deg, #e94560, #ff6b6b)`
- h1：24px 白色粗体，可含 `<br>` 换行（赛事编号 + 对阵）
- subtitle：15px #a0aec0
- match-tag：半透明圆角标签，14px
- **所有头部元素必须使用内联 style**（复制时取 `.container.innerHTML`，header 和 content 一起复制）

### 章节标题
- 左侧 4px 红色竖条 `#e94560`
- 编号使用圆形红色背景白色文字徽章（24×24px）

### 信息行（match-info）
- flex space-between 布局
- label 灰色 #888，value 粗体 #333
- 底部灰色分割线，最后一行无分割线（内联 style `border-bottom:none`）
- 所有元素使用内联 style

### 对阵 VS 卡（teams-box）
- 渐变色灰底 `linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%)`
- 圆角 10px
- VS 20px 粗体红 #e94560
- 队名 16px 粗体 #1a1a2e
- 联赛名 12px 灰 #888

### 分析卡片（analysis-card）
- 浅灰 #f8f9fa 底，圆角 8px
- h3 小标题 + 彩色 dot（红/蓝/橙）
- 列表项圆点改用 `<span>` 实现（替代 `::before` 伪元素）：`<span style="position:absolute;left:4px;color:#e94560;font-weight:bold;">·</span>`
- 所有卡片元素使用内联 style

### 高亮框（highlight-box）
- 浅红 #fff5f5 底 + 左侧 4px 红 #e94560 边框
- 圆角 6px
- 使用内联 style

### 数据统计条（stat-bar）
- flex 双色条，8px 高，圆角 4px
- 左侧 #e94560（客队/主队视角），右侧 #0f3460
- 标题 13px #666，数值标签 12px #888
- 使用内联 style

### 赔率表格（odds-table）
- 全宽、border-collapse
- 表头灰底 #f0f0f0
- 单元格 13px
- 所有 th/td 使用内联 style

### 预测框（prediction-box）
- 深渐变底 `linear-gradient(135deg, #1a1a2e 0%, #0f3460 100%)`
- 圆角 10px
- 标签 13px #a0aec0
- 主推荐 28px 粗体 #ff6b6b
- 比分徽章：半透明底 + 白色文字 22px
- 徽章 flex 居中排列
- 使用内联 style

### 免责声明（disclaimer）
- 黄底 #fffde7 或 #fff3cd
- 边框 1px solid #fff9c4
- 圆角 8px
- 文字 13px #795548
- 使用内联 style

## 生成说明

1. **头部级联**：品牌名（从原文"阿豹料码"等提取 → brand Badge）→ h1（赛事编号 + 对阵）→ subtitle（赛事阶段）→ match-tag（时间场地）
2. **章节编号**：从原文识别带编号的章节（0/1/2/3...），使用 section-title 组件，编号渲染为红色圆形徽章
3. **统计条**：识别控球率、射门数、射正数等百分比数据 → 渲染为双色 stat-bar
4. **赔率表**：识别赔率数据（多行×多列）→ 渲染为 odds-table
5. **预测框**：识别"阿豹推荐/结论"等内容 → 渲染为深色渐变预测框 + 比分徽章
6. **推荐列表**（竞彩/比分/指数）→ 可渲染为预测框内的结构化行，或使用三行列表
7. **所有 `.content` 内的元素必须使用内联 style**（含 font-family），不支持 CSS class。因为复制时只取 `.content` 的 innerHTML，class 样式会丢失
8. **`::before` 伪元素不能使用**，需要用 `<span>` 标签替代（如列表圆点）
9. 颜色对比度：正文 ≥ #444，辅助文字 ≥ #888

## 验证清单

### 渲染正确性
- [ ] 头部渐变 + Brand Badge + h1 + subtitle + match-tag
- [ ] 章节标题带红色编号圆形徽章 + 红色左边框
- [ ] 信息行 flex space-between 布局，最后行无分割线
- [ ] 对阵 VS 卡灰渐变底 + 圆角 + VS 红色
- [ ] 分析卡片灰底 + dot 小标题 + 列表
- [ ] 高亮框浅红底 + 红色左边框
- [ ] 数据统计条双色 8px 圆角
- [ ] 赔率表格全宽 + 灰底表头
- [ ] 预测框深渐变底 + 主推荐 28px + 比分徽章
- [ ] 章节间 margin-bottom: 32px
- [ ] **所有 `.container` 内元素使用内联 style（含头部、卡片、列表、表格）**
- [ ] **`::before` 伪元素已全部替换为 `<span>`**
- [ ] 复制取 `.container.innerHTML`，header + content 全部保留

### 功能正确性
- [ ] 页面在浏览器直接预览正常
- [ ] 复制按钮可用
