import json
from http.server import HTTPServer, BaseHTTPRequestHandler

class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        print(fmt % args)

    def _json(self, code, data):
        body = json.dumps({"code": code, "msg": "", "data": data}, ensure_ascii=False).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json;charset=UTF-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if "getAuthorByMobile" in self.path:
            self._json(0, None)
        elif "author/all" in self.path:
            # Assign-role author dropdown fallback; integration uses oa-server for real data.
            self._json(0, [])
        elif "author/simple-list" in self.path:
            self._json(0, [])
        elif self.path.startswith("/actuator/health"):
            self._json(0, {"status": "UP"})
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        if "updateAuthorLoginInfo" in self.path or "author" in self.path:
            self._json(0, True)
        else:
            self.send_response(404)
            self.end_headers()

if __name__ == "__main__":
    HTTPServer(("127.0.0.1", 48087), Handler).serve_forever()
