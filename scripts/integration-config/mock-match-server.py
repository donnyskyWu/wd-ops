#!/usr/bin/env python3
"""Minimal match-server stub for local integration when shenyu-match DB is unavailable.

Handles:
  GET /admin-api/match/lottery-schedule/getCzIssue  -> []
  GET /actuator/health                             -> UP
"""
import json
from http.server import BaseHTTPRequestHandler, HTTPServer


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
        path = self.path.split("?", 1)[0]
        if path.endswith("/lottery-schedule/getCzIssue"):
            self._json(0, [])
        elif path.startswith("/actuator/health"):
            self._json(0, {"status": "UP"})
        else:
            self.send_response(404)
            self.end_headers()


if __name__ == "__main__":
    HTTPServer(("127.0.0.1", 48088), Handler).serve_forever()
