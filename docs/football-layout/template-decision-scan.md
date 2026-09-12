# 决策扫读版模板（`decision-scan`）

> 适用场景：微信公众号/135编辑器/秀米粘贴，决策路径优化。设计目标：3 秒内让扫读者定位到"推谁赢"。

## 设计意图

### 核心模型：扫读决策者

读者行为模式是**三层渐进式扫读**：

```
第一层（0.5s）：定位比赛 → 扫比赛头（哪场？）
第二层（1.5s）：确认对象 → 扫对阵（谁打谁？）
第三层（0.5s）：获取结论 → 扫推荐（推谁？）
                    ↓
           不决策才回读正文
```

**排版资源分配：** 比赛前三级信息（哪场→谁打→推谁）占版面视觉权重的绝大多数，正文分析占次要权重。

### 生态适配

| 生态约束 | 设计决定 |
|---------|---------|
| 微信 135/秀米 编辑器 | 全部内联 style |
| 手机屏幕阅读（主场景） | max-width: 620px |
| 移动端最小可读字号 | 最小 14px，正文 16px |
| 跨平台 | PingFang / Microsoft YaHei |
| 复制粘贴 | textarea + execCommand('copy') |

## 色彩系统

| 颜色 | 排版功能 |
|------|---------|
| 深蓝 `#1a2a4a` | 标题、比赛头、小标题（主色调） |
| 红色 `#c0392b` | 推荐标签、决策信号（强调色） |
| 黄色背景 `#fff3cd` | 推荐胜负值背景高亮 |
| 深色 `#1a1a2e` | 对阵双方队名 |
| 柔和正文色 `#2a2a3e` | 正文段落 |
| 灰色 `#888898` | 最小可读灰阶 |
| 辅助色 `#6a7a96` | 时间/辅助说明 |
| 法律文本 `#b0b0c0` | 免责/版权 |

## 组件库

| 组件 | 标签 | 适用内容 |
|------|------|---------|
| 概念头部 Banner | `<div>` + `<h2>` | 品牌概念区 |
| 文章主标题 | `<h1>` | 全文最高层级标题 |
| 二级标题 | `<h2>` | 比赛头、联赛标记 |
| 三级标题 | `<h3>` | 卡片内小标题 |
| 对比表格 | `<table>`+`<thead>`+`<th>`+`<td>` | 结构化对比 |
| 引用块 | `<blockquote>`+`<footer>` | 带来源的引用 |
| 定义列表 | `<dl>`+`<dt>`+`<dd>` | 比赛元数据 |
| 有序列表 | `<ol>`+`<li>` | 递进步骤 |
| 无序列表 | `<ul>`+`<li>` | 推荐列表三行 |
| 图文说明 | `<figure>`+`<figcaption>` | 装饰标语 |
| 说明卡片（蓝） | `<div>` 渐变蓝底+蓝细边框 | 功能说明 |
| 说明卡片（红） | `<div>` 渐变红底+红细边框 | 功能说明 |
| 比赛预览卡片 | `<div>` 浅灰底+细边框 | 简约比赛信息 |
| 理念卡片 | `<div>` 浅蓝底+深蓝左边框 | 核心理念 |
| 职责卡片 | `<div>` 金底+金色左边框 | 职责定义 |
| 预测卡片 | `<div>` 红边框+浅红底 | 推荐列表+结论 |
| 金句卡片 | `<div>` 深蓝渐变 | 结尾升华 |
| 版权声明 | `<div>`+`<small>` | 法律/免责 |
| 分割线 | `<hr>` | 主要章节间 |

### 组件选择逻辑

```
读者想做什么？
├─ 定位文章主题 → <h1>
├─ 定位比赛 → <h2> 比赛头
├─ 对比维度 → <table> 表格
├─ 看推荐 → 预测卡片（红边框）
├─ 看元数据 → <dl> 定义列表
├─ 读引用 → <blockquote>
├─ 理解概念 → 卡片（理念/职责）
├─ 快速了解功能 → 说明卡片（蓝/红细边框）
├─ 看比赛预览 → 比赛预览卡片（浅灰底）
└─ 读分析 → <p> 段落
```

## 排版参数默认值

### 字体

| 参数名 | 默认值 | 说明 |
|--------|-------|------|
| title_font_size | 22 | 文章标题字号，px |
| title_color | #1a2a4a | 文章标题颜色 |
| match_header_font_size | 14 | 比赛头字号，px |
| match_header_color | #1a2a4a | 比赛头颜色 |
| team_vs_font_size | 18 | 对阵行字号，px |
| team_vs_color | #1a1a2e | 队名颜色 |
| vs_text_color | #888898 | VS文字颜色 |
| match_time_font_size | 14 | 比赛时间字号，px |
| match_time_color | #6a7a96 | 比赛时间颜色 |
| paragraph_font_size | 16 | 正文字号，px |
| paragraph_line_height | 1.95 | 正文行高，em |
| paragraph_color | #2a2a3e | 正文颜色 |
| rec_label_font_size | 15 | 推荐标签字号，px |
| rec_label_color | #c0392b | 推荐标签颜色 |
| rec_value_font_size | 15 | 推荐值字号，px |
| rec_value_color | #1a2a4a | 推荐值颜色（比分/指数） |
| rec_highlight_color | #c0392b | 推荐胜负值文字颜色 |
| rec_highlight_background | #fff3cd | 推荐胜负值背景高亮色 |
| rec_conclusion_font_size | 15 | 预测结论字号，px |
| subheading_font_size | 15 | 小标题字号，px |
| subheading_color | #1a2a4a | 小标题颜色 |
| footer_font_size | 13 | 页脚字号，px |
| footer_color | #8a8a98 | 页脚颜色 |
| image_placeholder_font_size | 14 | 图片占位文字号，px |
| image_placeholder_color | #6a7a96 | 图片占位字颜色 |
| toolbar_background_color | rgba(255,255,255,0.92) | 标题栏背景 |
| toolbar_title_color | #1a2a4a | 标题栏标题颜色 |

### 间距

| 参数名 | 默认值（px） |
|--------|-------------|
| paragraph_margin_bottom | 10 |
| paragraph_margin_top | 0 |
| rec_item_margin_bottom | 6 |
| rec_block_margin_top | 12 |
| block_margin_top | 30 |
| block_padding_top | 30 |
| section_margin_top | 20 |
| title_margin_bottom | 16 |
| block_separator_color | #d8dbe4 |

## HTML 模板

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>{文章标题} - 足球分析排版</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body {
    background: #f4f5f7;
    padding-top: 64px;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
  }
  #toolbar {
    position: fixed; top: 0; left: 0; right: 0; z-index: 9999;
    background: rgba(255,255,255,0.92);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    border-bottom: 1px solid rgba(0,0,0,0.04);
  }
  #toolbar-inner {
    display: flex; justify-content: space-between; align-items: center;
    max-width: 620px; margin: 0 auto; padding: 14px 20px;
  }
  #toolbar-title {
    font-size: 15px; font-weight: 500; color: #1a2a4a;
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    flex: 1; margin-right: 12px;
    letter-spacing: 0.5px; opacity: 0.8;
  }
  #copy-btn {
    background: #1a2a4a; color: #fff; border: none; border-radius: 6px;
    padding: 10px 22px; font-size: 13px; font-weight: 500;
    cursor: pointer; white-space: nowrap; flex-shrink: 0;
    transition: all 0.35s cubic-bezier(0.25,0.46,0.45,0.94);
    letter-spacing: 0.8px;
  }
  #copy-btn:hover { background: #2c4066; transform: translateY(-1px); box-shadow: 0 6px 20px rgba(26,42,74,0.2); }
  #copy-btn:active { transform: translateY(0); box-shadow: none; }
  #copy-btn.copied { background: #2d7d5a; }
  #toast {
    position: fixed; bottom: 36px; left: 50%; transform: translateX(-50%);
    background: rgba(26,26,46,0.88); color: #f0f0f0; padding: 10px 24px;
    border-radius: 8px; font-size: 13px; z-index: 99999;
    opacity: 0; transition: opacity 0.3s ease; pointer-events: none;
    white-space: nowrap; letter-spacing: 0.3px;
  }
  #toast.show { opacity: 1; }
  #article-wrapper {
    max-width: 620px; margin: 20px auto 48px;
    background: #fff;
    border-radius: 10px;
    overflow: hidden;
    box-shadow: 0 1px 2px rgba(0,0,0,0.03), 0 8px 24px rgba(0,0,0,0.04);
  }
</style>
</head>
<body>
  <div id="toolbar">
    <div id="toolbar-inner">
      <span id="toolbar-title">{文章标题}</span>
      <button id="copy-btn" onclick="copyFormatted()">复制全文</button>
    </div>
  </div>
  <div id="article-wrapper">
    <div id="article-content">
      <!-- 解析后的内容逐元素填充，每个元素独立内联 style -->
    </div>
  </div>
  <div id="toast"></div>
  <script>
    function copyFormatted() {
      var btn = document.getElementById('copy-btn');
      var el = document.getElementById('article-content');
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
        showToast('复制失败，请手动选中文章内容后复制');
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

### 文章标题 `<h1>`
左侧 3.5px 红色竖条装饰，字号 22px，颜色 #1a2a4a：
```html
<h1 style="font-family:-apple-system,'PingFang SC','Microsoft YaHei','Helvetica Neue',sans-serif;font-size:22px;font-weight:700;color:#1a2a4a;border-left:3.5px solid #c0392b;padding-left:14px;margin:0 0 20px 0;line-height:1.35;letter-spacing:0.5px;">文章标题</h1>
```

### 章节标题 `<h2>`（带编号的 section-title）
h2 左侧 3.5px 红色竖条，编号使用红色圆形徽章（24×24px），白色数字居中：
```html
<h2 style="font-family:-apple-system,'PingFang SC','Microsoft YaHei','Helvetica Neue',sans-serif;font-size:16px;font-weight:700;color:#1a2a4a;border-left:3.5px solid #c0392b;padding-left:12px;margin:0 0 14px 0;line-height:1.4;"><span style="display:inline-block;background:#c0392b;color:#fff;font-size:13px;width:24px;height:24px;line-height:24px;text-align:center;border-radius:50%;margin-right:8px;vertical-align:middle;">0</span>赛事档案</h2>
```
原文带编号章节（0/1/2/3/etc.）按原文编号渲染。原文无序号则不加编号。

### 预测结果卡片（强制）
红色边框（1.5px）+ 浅红底：
```html
<div style="font-family:-apple-system,'PingFang SC','Microsoft YaHei','Helvetica Neue',sans-serif;border:1.5px solid #c0392b;border-radius:8px;padding:18px 16px;margin:0 0 14px 0;background:#fdf6f6;">
  <ul style="font-family:...;list-style:none;margin:0;padding:0;">
    <li style="font-family:...;font-size:15px;line-height:2.0;margin:0 0 6px 0;padding:0;color:#2a2a3e;">
      <span style="font-weight:600;color:#c0392b;">竞彩推荐：</span>
      <span style="font-weight:600;color:#c0392b;background:#fff3cd;padding:1px 8px;border-radius:2px;font-size:16px;">负</span>
    </li>
    <li style="font-family:...;font-size:15px;line-height:2.0;margin:0 0 6px 0;padding:0;color:#2a2a3e;">
      <span style="font-weight:600;color:#c0392b;">比分推荐：</span>
      <span style="font-weight:500;color:#1a2a4a;font-size:15px;">0-2、0-3</span>
    </li>
    <li style="font-family:...;font-size:15px;line-height:2.0;margin:0 0 0 0;padding:0;color:#2a2a3e;">
      <span style="font-weight:600;color:#c0392b;">指数推荐：</span>
      <span style="font-weight:500;color:#1a2a4a;font-size:15px;">博德闪耀 -1.5</span>
    </li>
  </ul>
  <div style="font-family:...;margin-top:10px;padding-top:10px;border-top:1px dashed #e8dcdc;">
    <p style="font-family:...;font-size:15px;font-weight:500;color:#c0392b;margin:0;text-align:center;letter-spacing:0.5px;">
      ★ 综合推荐：<span style="background:#c0392b;color:#fff;padding:2px 14px;border-radius:4px;font-size:16px;font-weight:500;">负 · 博德闪耀 -1.5</span>
    </p>
  </div>
</div>
```

### 引用块
纯左侧 2px 红色竖线，无背景：
```html
<blockquote style="font-family:...;margin:0 0 14px 0;padding:0 0 0 16px;border-left:2px solid #c0392b;color:#5a5a72;font-size:15px;line-height:2.0;">
  <p style="font-family:...;margin:0 0 6px 0;">...</p>
  <footer style="font-family:...;font-size:13px;color:#888898;">—— 来源</footer>
</blockquote>
```

### 定义列表（比赛元数据）
```html
<dl style="font-family:...;display:flex;justify-content:center;gap:20px;margin:0 0 14px 0;">
  <div>
    <dt style="font-family:...;font-size:13px;color:#888898;">比赛时间</dt>
    <dd style="font-family:...;font-size:14px;color:#4a4a5a;margin:0;">2026-07-26 23:00</dd>
  </div>
</dl>
```

### 卡片类型

| 卡片用途 | 背景 | 边框 |
|---------|------|------|
| 说明卡片（蓝） | `linear-gradient(135deg,#f5f8fe,#edf2f9)` | 1px solid `#e3e8f0`，圆角 8px |
| 说明卡片（红） | `linear-gradient(135deg,#fdf6f6,#f8ecec)` | 1px solid `#f0e4e4`，圆角 8px |
| 核心理念 | `#f6f8fd` | 3.5px solid `#1a2a4a` 左侧 |
| 职责总结 | `linear-gradient(135deg,#fffcf2,#fef6e8)` | 3.5px solid `#d4a017` 左侧 |
| 辅助说明 | `#f9f9f9` | 无 |
| 结尾金句 | `linear-gradient(135deg,#162240,#1f3460)` | 圆角 10px，文字 `#f0e6c0` |
| 比赛预览 | `#f8f9fc` | 1px solid `#edeff2`，圆角 8px |
| 预测结果 | `#fdf6f5` | 1.5px solid `#c0392b`，圆角 8px |

### 分隔线
```html
<hr style="border:none;text-align:center;margin:0 0 24px 0;font-size:14px;color:#d8dbe4;letter-spacing:6px;" data-content="— — —">
```

## 段内高亮具体颜色

使用本模板"排版参数默认值"中的颜色值，具体映射：

| 高亮类型 | span 模板 |
|---------|----------|
| 蓝色加粗 | `<span style="font-weight:600;color:#1a2a4a;">{文本}</span>` |
| 红色加粗 | `<span style="font-weight:600;color:#c0392b;">{文本}</span>` |
| 红字+黄底 | `<span style="font-weight:600;color:#c0392b;background:#fff3cd;padding:0 4px;border-radius:2px;">{文本}</span>` |
| 黄底+加粗 | `<span style="font-weight:600;background:#fff3cd;padding:0 4px;border-radius:2px;">{文本}</span>` |

## 生成说明

1. 文章标题提取后填入 `<title>`、`#toolbar-title` 和内容区 `<h1>`
2. 无标题时 toolbar-title 显示"足球分析文章"
3. 所有可见标签的内联 style 中独立包含完整 font-family 字体栈
4. 段落内混合 ≥2 种视觉增强手段
5. **章节标题带编号**：原文有编号（0/1/2/3/4/5/6/7等）的 h2 章节，渲染为红色圆形徽章（24×24px）+ 编号白色数字居中。编号从原文提取
6. 主要章节之间用 `— — —` 风格 `<hr>` 分割
7. padding: 36px 24px 48px

## 验证清单

### 渲染正确性
- [ ] 文章标题 22px 粗体深蓝 + 左侧 3.5px 红色竖条
- [ ] 正文 ≥ 16px、行高 ≥ 1.95、颜色 #2a2a3e
- [ ] 最小字号 ≥ 14px
- [ ] 所有可读文本颜色 ≥ #888898
- [ ] 每个段落混合 ≥2 种视觉增强
- [ ] 章节 h2 带红色圆形编号徽章（0-7等，按原文编号）
- [ ] 比赛头 14px 粗体深蓝
- [ ] 对阵行 18px 粗体 #1a1a2e，VS 灰色
- [ ] 推荐列表为红色边框预测卡片
- [ ] 胜负值 16px 红字黄底，比分/指数 15px 深蓝
- [ ] 预测卡片底部有结论汇总行
- [ ] 引用块使用 `<blockquote>`+`<footer>`，纯左侧红色竖线
- [ ] 元数据使用 `<dl>`+`<dt>`+`<dd>`
- [ ] 对比数据使用 `<table>`
- [ ] 所有可见标签独立 font-family
- [ ] 章节间 `— — —` 分割线
- [ ] 顶部标题栏 fixed + 毛玻璃
- [ ] 复制按钮、Toast 提示、copyFormatted 函数

### 功能正确性
- [ ] 复制格式保留（编辑器源代码模式粘贴）
- [ ] 复制成功按钮变绿 2 秒后恢复
