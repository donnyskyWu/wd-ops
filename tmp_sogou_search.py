import re, urllib.parse, json, time
from curl_cffi import requests

def sogou_search(query, page=1):
    q = urllib.parse.quote(query)
    url = f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8&page={page}"
    headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}
    r = requests.get(url, headers=headers, impersonate="chrome", timeout=30)
    html = r.text
    print("status", r.status_code, "len", len(html))
    items = []
    for m in re.finditer(r'<h3>\s*<a[^>]*href="([^"]+)"[^>]*uigs="article_title_(\d+)"[^>]*>(.*?)</a>', html, re.S):
        href, idx, title = m.group(1), m.group(2), re.sub(r"<[^>]+>", "", m.group(3)).strip()
        items.append({"title": title, "sogou_href": href, "idx": idx})
    # fallback: simpler pattern
    if not items:
        for m in re.finditer(r'uigs="article_title_\d+"[^>]*href="([^"]+)"[^>]*>([^<]+)<', html):
            items.append({"title": m.group(2).strip(), "sogou_href": m.group(1)})
    print("items", len(items))
    for it in items[:12]:
        print(json.dumps(it, ensure_ascii=False))
    return items, html[:500]

sogou_search("雷速体育APP")
