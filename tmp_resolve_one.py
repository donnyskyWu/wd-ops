import urllib.parse
from curl_cffi import requests
import re, html
href="/link?url=dn9a_-gY295K0Rci_xozVXfdMkSQTLW6cwJThYulHEtVjXrGTiVgS-uWX6u8fIhvQ6K4Gj4lr9HeOBlvIbXr6VqXa8Fplpd9VQZhmB_9VqnwjiXsPULzc_lZGEOcXCAEzDaHfQZPz3n_ZcYJz0bCBTE03DLCSErUIqmcCqGkf9FxYzb4H5BccS0zysTrkEPm1gGM7RE6nccCaYuoJYtOAcCFjq-zwx3HbsmiV1yZel3tGIUxl4TKulbcK4Gm6MX6WUzQtcl0Alt535pjGOOjYg..&type=2&query=%E9%9B%B7%E9%80%9F%E4%BD%93%E8%82%B2APP&token=4337E54396FF970FA0A6F6F99DE8C5D7A043DE2D6A535461"
full="https://weixin.sogou.com"+href
r=requests.get(full, headers={"User-Agent":"Mozilla/5.0"}, impersonate="chrome", timeout=30, allow_redirects=True)
print(r.url)
